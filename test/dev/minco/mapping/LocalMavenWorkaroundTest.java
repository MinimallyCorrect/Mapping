package dev.minco.mapping;

import java.nio.file.Paths;

import lombok.val;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class LocalMavenWorkaroundTest {
	@Test
	void testResolvedPathForArtifact() {
		val base = Paths.get("./");
		val resolved = LocalMavenWorkaround.resolveFilePathInRepo(base, "com.example.maven", "Maven", "1.1-SNAPSHOT");
		val expected = base.resolve("com").resolve("example").resolve("maven").resolve("Maven").resolve("1.1-SNAPSHOT").resolve("Maven-1.1-SNAPSHOT");
		Assertions.assertEquals(expected, resolved);
	}

	@Test
	void testGeneratePom() {
		val contents = LocalMavenWorkaround.generatePom("com.example.maven", "Maven", "1.1-SNAPSHOT", false).replace("\r", "");
		val expected = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
			"<project>\n" +
			"    <modelVersion>4.0.0</modelVersion>\n" +
			"    <groupId>com.example.maven</groupId>\n" +
			"    <artifactId>Maven</artifactId>\n" +
			"    <version>1.1-SNAPSHOT</version>\n" +
			"</project>\n";
		Assertions.assertEquals(expected, contents);
	}
}
