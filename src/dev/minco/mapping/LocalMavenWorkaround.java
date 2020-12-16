package dev.minco.mapping;

import java.io.File;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import lombok.SneakyThrows;

public class LocalMavenWorkaround {
	@SneakyThrows
	public static void createLocalMavenRepoForSingleFile(File baseDir, File existingFile, String group, String name, String version) {
		var artifact = resolveFilePathInRepo(baseDir.toPath(), group, name, version);
		var jar = artifact.resolveSibling(artifact.getFileName() + ".jar");

		if (Files.exists(jar)) {
			return;
		}

		var pom = artifact.resolveSibling(artifact.getFileName() + ".pom");

		Files.createDirectories(artifact.getParent());
		Files.copy(existingFile.toPath(), jar);
		Files.writeString(pom, generatePom(group, name, version));
		// Example "repo/com/google/thing/Thing"
	}

	public static Path resolveFilePathInRepo(Path baseDir, String group, String name, String version) {
		return baseDir.resolve(group.replace('.', '/')).resolve(name).resolve(version).resolve(name + "-" + version);
	}

	@SneakyThrows
	public static String generatePom(String group, String artifact, String version) {
		var doc = DocumentBuilderFactory.newDefaultInstance().newDocumentBuilder().newDocument();
		var root = doc.createElement("project");
		doc.appendChild(root);

		var mv = doc.createElement("modelVersion");
		mv.setTextContent("4.0.0");
		root.appendChild(mv);

		var groupId = doc.createElement("groupId");
		groupId.setTextContent(group);
		root.appendChild(groupId);

		var artifactId = doc.createElement("artifactId");
		artifactId.setTextContent(artifact);
		root.appendChild(artifactId);

		var versionElement = doc.createElement("version");
		versionElement.setTextContent(version);
		root.appendChild(versionElement);

		var sw = new StringWriter();
		var transformer = TransformerFactory.newInstance().newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.transform(new DOMSource(doc), new StreamResult(sw));
		return sw.toString();
	}
}
