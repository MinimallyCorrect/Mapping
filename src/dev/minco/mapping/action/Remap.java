package dev.minco.mapping.action;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import javax.annotation.Nullable;

import dev.minco.mapping.util.Throw;
import net.fabricmc.mapping.tree.*;
import net.fabricmc.tinyremapper.IMappingProvider;
import net.fabricmc.tinyremapper.OutputConsumerPath;
import net.fabricmc.tinyremapper.TinyRemapper;

public final class Remap {
	private Remap() {}

	public static void run(Iterable<File> mappings, File input, File output, @Nullable String packageToMap) {
		System.err.println("Remapping {" + input + "} to {" + output + "} for package {" + packageToMap + "}");

		var remapper = TinyRemapper.newRemapper()
			.withMappings(createFromFabric(mappings.iterator().next())) // TODO
			.build();

		try (var outputConsumer = new OutputConsumerPath.Builder(output.toPath()).build()) {
			outputConsumer.addNonClassFiles(input.toPath());
			remapper.readInputs(input.toPath());
			remapper.apply(outputConsumer);
		} catch (IOException e) {
			throw Throw.sneaky(e);
		} finally {
			remapper.finish();
		}
	}

	private static IMappingProvider createFromFabric(File file) {
		try (var br = Files.newBufferedReader(file.toPath())) {
			TinyTree mapping = TinyMappingFactory.loadWithDetection(br);
			var nses = mapping.getMetadata().getNamespaces();
			System.out.println("file = " + file);
			System.out.println(nses);
			return createFromTinyTree(mapping, nses.get(0), nses.get(1), true);
		} catch (IOException e) {
			throw Throw.sneaky(e);
		}
	}

	// copied from https://github.com/FabricMC/fabric-loom/blob/8e916f8fb0523ee3a516f0b5a2606d344dd0b414/src/main/java/net/fabricmc/loom/util/TinyRemapperMappingsHelper.java#L42
	private static IMappingProvider createFromTinyTree(TinyTree mappings, String from, String to, boolean remapLocalVariables) {
		return (acceptor) -> {
			for (ClassDef classDef : mappings.getClasses()) {
				String className = classDef.getName(from);
				acceptor.acceptClass(className, classDef.getName(to));

				for (FieldDef field : classDef.getFields()) {
					acceptor.acceptField(memberOf(className, field.getName(from), field.getDescriptor(from)), field.getName(to));
				}

				for (MethodDef method : classDef.getMethods()) {
					IMappingProvider.Member methodIdentifier = memberOf(className, method.getName(from), method.getDescriptor(from));
					acceptor.acceptMethod(methodIdentifier, method.getName(to));

					if (remapLocalVariables) {
						for (ParameterDef parameter : method.getParameters()) {
							acceptor.acceptMethodArg(methodIdentifier, parameter.getLocalVariableIndex(), parameter.getName(to));
						}

						for (LocalVariableDef localVariable : method.getLocalVariables()) {
							acceptor.acceptMethodVar(methodIdentifier, localVariable.getLocalVariableIndex(),
								localVariable.getLocalVariableStartOffset(), localVariable.getLocalVariableTableIndex(),
								localVariable.getName(to));
						}
					}
				}
			}
		};
	}

	private static IMappingProvider.Member memberOf(String className, String memberName, String descriptor) {
		return new IMappingProvider.Member(className, memberName, descriptor);
	}
}
