package dev.minco.mapping;

import java.net.URL;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.repositories.IvyArtifactRepository;

public class GradleExtensions {
	public static Configuration createSingleFileConfigurationForUrl(Project project, URL url) {
		var name = url.getFile();
		var repoUrl = url.getProtocol() + "://" + url.getHost();
		project.getRepositories().ivy((IvyArtifactRepository repo) -> {
			repo.setName("Single target repo for " + url.getPath());
			repo.setUrl(repoUrl);
			repo.patternLayout(ivyPatternRepositoryLayout -> {
				ivyPatternRepositoryLayout.artifact(url.getPath());
			});
			repo.metadataSources(IvyArtifactRepository.MetadataSources::artifact);
			repo.content(repositoryContentDescriptor -> {
				repositoryContentDescriptor.includeVersion("byurl", name, "1");
			});
		});

		return project.getConfigurations().detachedConfiguration(project.getDependencies().create("byurl:" + name + ":1"));
	}
}
