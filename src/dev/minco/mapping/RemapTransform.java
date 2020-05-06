package dev.minco.mapping;

import org.gradle.api.artifacts.transform.TransformAction;
import org.gradle.api.artifacts.transform.TransformOutputs;
import org.gradle.api.artifacts.transform.TransformParameters;
import org.gradle.api.file.ConfigurableFileCollection;

public abstract class RemapTransform implements TransformAction<RemapTransform.RemapParameters> {
	@Override
	public void transform(TransformOutputs outputs) {
		throw new RuntimeException("TODO"); // TODO
	}

	interface RemapParameters extends TransformParameters {
		ConfigurableFileCollection getMappings();
	}
}
