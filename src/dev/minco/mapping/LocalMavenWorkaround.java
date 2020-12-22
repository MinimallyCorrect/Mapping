package dev.minco.mapping;

import java.io.File;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.annotation.Nullable;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import lombok.SneakyThrows;

public class LocalMavenWorkaround {
	@SneakyThrows
	public static void createLocalMavenRepoForSingleFile(File baseDir, File existingFile, String group, String name, String version, @Nullable String setModuleMetadata) {
		var artifact = resolveFilePathInRepo(baseDir.toPath(), group, name, version);
		var jar = artifact.resolveSibling(artifact.getFileName() + ".jar");

		var pom = artifact.resolveSibling(artifact.getFileName() + ".pom");
		var module = artifact.resolveSibling(artifact.getFileName() + ".module");

		Files.createDirectories(artifact.getParent());

		Files.deleteIfExists(jar);
		Files.deleteIfExists(pom);
		Files.deleteIfExists(module);

		Files.copy(existingFile.toPath(), jar);
		Files.writeString(pom, generatePom(group, name, version, setModuleMetadata != null));
		if (setModuleMetadata != null) {
			Files.writeString(module, generateModule(group, name, version, jar.getFileName().toString(), setModuleMetadata));
		}
		// Example "repo/com/google/thing/Thing"
	}

	public static Path resolveFilePathInRepo(Path baseDir, String group, String name, String version) {
		return baseDir.resolve(group.replace('.', '/')).resolve(name).resolve(version).resolve(name + "-" + version);
	}

	@SneakyThrows
	public static String generatePom(String group, String artifact, String version, boolean addRedirect) {
		var doc = DocumentBuilderFactory.newDefaultInstance().newDocumentBuilder().newDocument();
		var root = doc.createElement("project");
		doc.appendChild(root);

		if (addRedirect) {
			var comment = doc.createComment(" do_not_remove: published-with-gradle-metadata ");
			root.appendChild(comment);
		}

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

	public static String generateModule(String group, String artifact, String version, String fileName, String setModuleMetadata) {
		return "{\n" +
			"  \"formatVersion\": \"1.1\",\n" +
			"  \"component\": {\n" +
			"    \"group\": \"" + group + "\",\n" +
			"    \"module\": \"" + artifact + "\",\n" +
			"    \"version\": \"" + version + "\",\n" +
			"    \"attributes\": {\n" +
			"      \"org.gradle.status\": \"integration\"\n" +
			"    }\n" +
			"  },\n" +
			"  \"createdBy\": {\n" +
			"    \"gradle\": {\n" +
			"      \"version\": \"6.7.1\",\n" +
			"      \"buildId\": \"onaumdmpkfcvpic43xrlto7fay\"\n" +
			"    }\n" +
			"  },\n" +
			"  \"variants\": [\n" +
			"    {\n" +
			"      \"name\": \"apiElements\",\n" +
			"      \"attributes\": {\n" +
			"        \"org.gradle.category\": \"library\",\n" +
			"        \"org.gradle.dependency.bundling\": \"external\",\n" +
			"        \"org.gradle.jvm.version\": 11,\n" +
			"        \"org.gradle.libraryelements\": \"jar\",\n" +
			"        \"org.gradle.usage\": \"java-api\",\n" +
			"        \"dev.minco.mapped.net.minecraft\": \"" + setModuleMetadata + "\"\n" +
			"      },\n" +
			"      \"files\": [\n" +
			"        {\n" +
			"          \"name\": \"" + fileName + "\",\n" +
			"          \"url\": \"" + fileName + "\",\n" +
			"          \"size\": 22492,\n" +
			"          \"sha512\": \"9004e76811fdcf40993c04a9ecad7f300af6a0a7c9449ce71fa854d29e2189ea61c25e95d864e4280bf07e9104791d638bdb16440fe1871b5d83a1cbd9bcd053\",\n" +
			"          \"sha256\": \"c2d42300d4cdc342bc1ba28cab380016adf0622cd89e1ca5983b0defb122a319\",\n" +
			"          \"sha1\": \"560a78be0ea434230cb8731c184a80df6a71a736\",\n" +
			"          \"md5\": \"6d296ccdf5bf3dc260f2607c833ea0a7\"\n" +
			"        }\n" +
			"      ]\n" +
			"    },\n" +
			"    {\n" +
			"      \"name\": \"runtimeElements\",\n" +
			"      \"attributes\": {\n" +
			"        \"org.gradle.category\": \"library\",\n" +
			"        \"org.gradle.dependency.bundling\": \"external\",\n" +
			"        \"org.gradle.jvm.version\": 11,\n" +
			"        \"org.gradle.libraryelements\": \"jar\",\n" +
			"        \"org.gradle.usage\": \"java-runtime\",\n" +
			"        \"dev.minco.mapped.net.minecraft\": \"" + setModuleMetadata + "\"\n" +
			"      },\n" +
			"      \"dependencies\": [],\n" +
			"      \"files\": [\n" +
			"        {\n" +
			"          \"name\": \"" + fileName + "\",\n" +
			"          \"url\": \"" + fileName + "\",\n" +
			"          \"size\": 22492,\n" +
			"          \"sha512\": \"9004e76811fdcf40993c04a9ecad7f300af6a0a7c9449ce71fa854d29e2189ea61c25e95d864e4280bf07e9104791d638bdb16440fe1871b5d83a1cbd9bcd053\",\n" +
			"          \"sha256\": \"c2d42300d4cdc342bc1ba28cab380016adf0622cd89e1ca5983b0defb122a319\",\n" +
			"          \"sha1\": \"560a78be0ea434230cb8731c184a80df6a71a736\",\n" +
			"          \"md5\": \"6d296ccdf5bf3dc260f2607c833ea0a7\"\n" +
			"        }\n" +
			"      ]\n" +
			"    }\n" +
			"  ]\n" +
			"}\n";
	}
}
