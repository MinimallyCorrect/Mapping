package dev.minco.mapping.task;

import org.gradle.api.Task;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.minco.mapping.action.Remap;

public abstract class RemapTask implements Task {
	@NotNull
	@Input
	abstract Property<@Nullable String> getPackageToMap();

	@PathSensitive(PathSensitivity.NAME_ONLY)
	@InputFiles
	abstract ConfigurableFileCollection getMappings();

	@Classpath
	abstract RegularFileProperty getInput();

	@OutputFile
	abstract RegularFileProperty getOutput();

	@TaskAction
	public void run() {
		var mappings = getMappings().getFiles();
		var input = getInput().get().getAsFile();
		var output = getOutput().get().getAsFile();
		var packageToMap = getPackageToMap().getOrNull();

		Remap.run(mappings, input, output, packageToMap);
	}
}
