package dev.minco.mapping;

import org.gradle.api.artifacts.transform.InputArtifact;
import org.gradle.api.artifacts.transform.TransformAction;
import org.gradle.api.artifacts.transform.TransformOutputs;
import org.gradle.api.artifacts.transform.TransformParameters;
import org.gradle.api.file.FileSystemLocation;
import org.gradle.api.provider.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class RemapTransform implements TransformAction<RemapTransform.Parameters> {
	private static final Logger logger = LoggerFactory.getLogger(RemapTransform.class);

	@InputArtifact
	public abstract Provider<FileSystemLocation> getInputArtifact();

	@Override
	public void transform(TransformOutputs outputs) {
		System.err.println("Called RemapTransform.transform");
		throw new RuntimeException("TODO: Actually transform. Expected failure."); // TODO
	}

	public interface Parameters extends TransformParameters {
		// TODO
	}
}
