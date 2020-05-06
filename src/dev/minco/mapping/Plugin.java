package dev.minco.mapping;

import org.gradle.api.Project;

@SuppressWarnings("unused")
public class Plugin implements org.gradle.api.Plugin<Project> {
	@Override
	public void apply(Project target) {
		target.getExtensions().create("mapping", MappingExtension.class);
	}
}
