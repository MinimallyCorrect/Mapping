package dev.minco.mapping;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ResolutionStrategy;
import org.gradle.api.artifacts.type.ArtifactTypeDefinition;
import org.gradle.api.attributes.Attribute;
import org.gradle.api.attributes.AttributeCompatibilityRule;
import org.gradle.api.attributes.CompatibilityCheckDetails;
import org.gradle.api.file.FileCollection;
import org.gradle.api.provider.MapProperty;

import dev.minco.mapping.util.Throw;
import net.fabricmc.mapping.reader.v2.TinyV2Factory;

@SuppressWarnings("UnstableApiUsage")
public abstract class MappingExtension {
	/**
	 * indicates a dependency valid for ANY mapping used for unknown mapping, eg dependencies which don't specifiy their mapping
	 */
	public static final String UNKNOWN_MAPPING = "UNKNOWN";
	Attribute<String> artifactType = Attribute.of("artifactType", String.class);

	protected abstract MapProperty<String, FileCollection> getRegisteredMappings();

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

		project.getLogger().lifecycle("configs " + configurations);
		applyMappingsToConfiguration(project, target, mapping, configurations);

		project.afterEvaluate((proj) -> applyMappingTransformersToConfigs(proj, target, mapping, configurations));
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
				System.err.println("made " + cfg + " have attr " + mapped + "=" + mapping);
				cfg.getAttributes().attribute(mapped, mapping);
			}
		});

		getRegisteredMappings().finalizeValue();
		getRegisteredMappings().get().forEach((k, v) -> {

		});

		project.getLogger().lifecycle("added transformers to configs" + configs);
	}

	private void applyMappingsToConfiguration(Project project, String target, String mapping, Collection<String> configurations) {
		var mapped = getMappingArtifactForTarget(target);

		var schema = project.getDependencies().getAttributesSchema();
		if (!schema.hasAttribute(mapped)) {
			schema.attribute(mapped).getCompatibilityRules().add(MappedAttributeCompatibilityRule.class);
			project.getDependencies().getArtifactTypes().getByName(ArtifactTypeDefinition.JAR_TYPE).getAttributes().attribute(mapped, UNKNOWN_MAPPING);
		}

		for (Configuration configuration : project.getConfigurations()) {
			if (!configurations.contains(configuration.getName())) {
				continue;
			}

			configuration.getAttributes().attribute(mapped, mapping);
		}

		project.getLogger().lifecycle("applied attrs to configs " + configurations);
	}

	private static List<String> listMappings(File mapping) {
		try (var reader = Files.newBufferedReader(mapping.toPath())) {
			return TinyV2Factory.readMetadata(reader).getNamespaces();
		} catch (IOException e) {
			throw Throw.sneaky(e);
		}
	}

	public static Attribute<String> getMappingArtifactForTarget(String target) {
		return Attribute.of("dev.minco.mapped." + target, String.class);
	}

	public static class MappedAttributeCompatibilityRule implements AttributeCompatibilityRule<String> {
		@Override
		public void execute(CompatibilityCheckDetails<String> details) {
			System.err.println("Compatibility check ran, values: " + details.getProducerValue() + " -> " + details.getConsumerValue());
			if (UNKNOWN_MAPPING.equals(details.getProducerValue())) {
				details.compatible();
			}
		}
	}
}
