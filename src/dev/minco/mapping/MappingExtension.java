package dev.minco.mapping;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import lombok.SneakyThrows;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ResolutionStrategy;
import org.gradle.api.artifacts.type.ArtifactTypeDefinition;
import org.gradle.api.attributes.Attribute;
import org.gradle.api.attributes.AttributeCompatibilityRule;
import org.gradle.api.attributes.CompatibilityCheckDetails;
import org.gradle.api.file.FileCollection;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

import net.fabricmc.mapping.reader.v2.TinyV2Factory;
import net.fabricmc.mapping.tree.TinyMappingFactory;

@SuppressWarnings("UnstableApiUsage")
public abstract class MappingExtension {
	/**
	 * indicates a dependency valid for ANY mapping used for unknown mapping, eg dependencies which don't specifiy their mapping
	 */
	public static final String UNKNOWN_MAPPING = "UNKNOWN";
	private static final Attribute<String> artifactType = Attribute.of("artifactType", String.class);

	protected abstract ListProperty<Function<String, String>> getMappingNamespaceTranslators();

	protected abstract MapProperty<String, FileCollection> getRegisteredMappings();

	public void registerNamespaceTranslator(Function<String, String> namespaceTranslator) {
		getMappingNamespaceTranslators().add(namespaceTranslator);
	}

	public String registerMapping(Project project, String target, String coord) {
		var registeredName = target + ':' + coord;
		var dep = project.getDependencies().create(coord);
		var conf = project.getConfigurations().detachedConfiguration(dep);
		conf.setTransitive(false);
		conf.resolutionStrategy(ResolutionStrategy::failOnVersionConflict);
		getRegisteredMappings().put(registeredName, conf.fileCollection(element -> true));
		return registeredName;
	}

	public void registerDefaultMappedConfiguration(Project project, String target, String mapping) {
		registerMappedConfiguration(project, null, target, mapping);
	}

	public void registerMappedConfiguration(Project project, @Nullable String configurationBaseName, String target, String mapping) {
		var conf = project.getConfigurations();

		List<String> configurations;
		if (configurationBaseName != null) {
			configurations = new ArrayList<>();
			for (String cfg : Configurations.builtinCfgPrefixes) {
				var name = configurationBaseName + Configurations.makeFirstUpper(cfg);
				configurations.add(name);
				var impl = conf.create(name);
				conf.getByName(cfg).extendsFrom(impl);
				impl.setCanBeConsumed(false);
				impl.setVisible(false);
			}
		} else {
			configurations = Configurations.builtinCfgPrefixes;
		}

		applyMappingsToConfiguration(project, target, mapping, configurations);

		applyMappingTransformersToConfigs(project, target, mapping, configurations);
		//project.afterEvaluate((proj) -> applyMappingTransformersToConfigs(proj, target, mapping, configurations));
	}

	private static boolean findExtendsFromInTree(Configuration base, List<Configuration> targets) {
		if (targets.contains(base)) {
			return true;
		}

		for (Configuration extended : base.getExtendsFrom()) {
			if (findExtendsFromInTree(extended, targets)) {
				return true;
			}
		}

		return false;
	}

	private void applyMappingTransformersToConfigs(Project project, String target, String mapping, Collection<String> configurations) {
		// Don't try to apply mappings if build already failed as this requires resolving deps
		if (project.getState().getFailure() != null) {
			return;
		}

		var mapped = getMappingArtifactForTarget(target);

		List<Configuration> configs = new ArrayList<>();
		for (Configuration configuration : project.getConfigurations()) {
			if (!configurations.contains(configuration.getName())) {
				continue;
			}

			configs.add(configuration);
		}

		project.getConfigurations().all(cfg -> {
			if (findExtendsFromInTree(cfg, configs)) {
				cfg.getAttributes().attribute(mapped, mapping);
			}
		});

		getMappingNamespaceTranslators().finalizeValue();
		getRegisteredMappings().finalizeValue();
		getRegisteredMappings().get().forEach((k, v) -> {
			var prefix = target + ':';
			if (!k.startsWith(prefix)) {
				return;
			}
			var mappingCoord = k.substring(prefix.length());

			project.getLogger().debug("Getting transformations for " + k + " -> " + v.getSingleFile());
			var mappings = listMappings(v.getSingleFile()).stream().map(it -> mappingCoord + ":" + it).collect(Collectors.toList());
			var simplified = mappings.stream().map(it -> {
				for (Function<String, String> stringStringFunction : getMappingNamespaceTranslators().get()) {
					it = MoreObjects.firstNonNull(stringStringFunction.apply(it), it);
				}
				return it;
			}).distinct().collect(Collectors.toList());

			if (mappings.size() != simplified.size()) {
				throw new RuntimeException("Multiple mapping namespaces in the same mapping file simplified to the same entry." +
					"\nInput: " + mappings + "" +
					"\nOutput: " + simplified);
			}

			project.getLogger().debug("Found mapped namespaces to set up transforms: " + mappings);

			for (String a : simplified) {
				for (String b : simplified) {
					if (!a.equals(b)) {
						registerSingleTransform(project, v, ArtifactTypeDefinition.JAR_TYPE, mapped, a, b);
						registerSingleTransform(project, v, ArtifactTypeDefinition.JVM_CLASS_DIRECTORY, mapped, a, b);

						project.getLogger().lifecycle("\nRegistered transform for attr " + mapped + " from:\n\t" + a + " -> " + b);
					}
				}
			}
		});
	}

	private void registerSingleTransform(Project project, FileCollection mappings, String type, Attribute<String> mapped, String a, String b) {
		project.getDependencies().registerTransform(RemapTransform.class, transformSpec -> {
			transformSpec.getFrom().attribute(artifactType, type).attribute(mapped, a);
			transformSpec.getTo().attribute(artifactType, type).attribute(mapped, b);
			transformSpec.parameters(parameters -> {});
		});
	}

	private void applyMappingsToConfiguration(Project project, String target, String mapping, Collection<String> configurations) {
		var mapped = getMappingArtifactForTarget(target);

		var schema = project.getDependencies().getAttributesSchema();
		if (!schema.hasAttribute(mapped)) {
			schema.attribute(mapped).getCompatibilityRules().add(MappedAttributeCompatibilityRule.class);
			String defaultAttrValue = System.getProperty("dev.minco.mapping.defaultAttrValueTest");
			if (defaultAttrValue == null) {
				defaultAttrValue = UNKNOWN_MAPPING;
			}
			project.getDependencies().getArtifactTypes().getByName(ArtifactTypeDefinition.JAR_TYPE).getAttributes().attribute(mapped, defaultAttrValue);
		}

		for (Configuration configuration : project.getConfigurations()) {
			if (!configurations.contains(configuration.getName())) {
				continue;
			}

			if (configuration.getAttributes().contains(mapped)) {
				throw new RuntimeException("Mapped attribute already set. Attr: " + mapped + " current value " + configuration.getAttributes().getAttribute(mapped) + " wanted " + mapping);
			}
			configuration.getAttributes().attribute(mapped, mapping);
		}

		project.getLogger().debug("applied attrs to configs " + configurations);
	}

	@SneakyThrows
	private static List<String> listMappings(File mapping) {
		try (var fs = FileSystems.newFileSystem(mapping.toPath(), (ClassLoader) null);
			var reader = Files.newBufferedReader(fs.getPath("mappings", "mappings.tiny"))) {

			reader.mark(8192);
			String firstLine = reader.readLine();
			String[] header = firstLine.split("\t");
			reader.reset();

			if (header[0].equals("v1")) {
				return TinyMappingFactory.loadLegacy(reader).getMetadata().getNamespaces();
			}

			return TinyV2Factory.readMetadata(reader).getNamespaces();
		}
	}

	public static Attribute<String> getMappingArtifactForTarget(String target) {
		return Attribute.of("dev.minco.mapped." + target, String.class);
	}

	public static class MappedAttributeCompatibilityRule implements AttributeCompatibilityRule<String> {
		private static final Logger logger = LoggerFactory.getLogger(MappedAttributeCompatibilityRule.class);

		@Override
		public void execute(CompatibilityCheckDetails<String> details) {
			var producer = details.getProducerValue();
			var consumer = details.getConsumerValue();
			boolean passed = UNKNOWN_MAPPING.equals(producer) || Objects.equals(producer, consumer);
			if (passed) {
				details.compatible();
			} else {
				details.incompatible();
			}
			logger.debug("Compatibility check ran, values: " + details.getProducerValue() + " -> " + details.getConsumerValue() + " = " + passed);
		}
	}
}
