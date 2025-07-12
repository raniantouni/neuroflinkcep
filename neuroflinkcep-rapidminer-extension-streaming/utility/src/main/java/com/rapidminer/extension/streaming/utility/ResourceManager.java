/*
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.utility;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.rapidminer.extension.streaming.utility.graph.StreamGraph;


/**
 * Utility class responsible for handling "resources"/artifacts in the job "fat-"JAR.
 * It also provides some generic utilities for dealing with resources, e.g.: create temporary file.
 *
 * @author Mate Torok
 * @since 0.4.0
 */
public final class ResourceManager {

	public static final String ARTIFACTS_FOLDER = "rm_job_artifacts";

	public static final String JAR_RESOURCE_PREFIX = "jar:";

	public static final String EXT_STREAM_GRAPH_EXT = ".graph";

	public static final String EXT_JAR = ".jar";

	private ResourceManager() {}

	/**
	 * This method returns the resource/artifact as a stream from "this" JAR
	 *
	 * @param fileName of the artifact to be retrieved
	 * @return input-stream for the desired resource
	 */
	public static InputStream retrieve(String fileName) {
		String fullPath = Paths.get(ARTIFACTS_FOLDER).resolve(fileName).toString();
		return ResourceManager.class.getClassLoader().getResourceAsStream(fullPath);
	}

	/**
	 * Deserializes and retrieves the graph artifact
	 *
	 * @param fileName artifact name
	 * @return deserialized StreamGraph
	 * @throws IOException
	 */
	public static StreamGraph retrieveGraph(String fileName) throws IOException {
		// Graph file sits in the JAR
		InputStream graphStream = ResourceManager.retrieve(fileName);
		if (graphStream == null) {
			throw new IOException("Graph not found: " + fileName);
		}

		// Deserialize file(-stream)
		return new KryoHandler().read(graphStream, StreamGraph.class);
	}

	/**
	 * Embeds files into a copy of the original JAR.
	 * Since the structure is flattened, it is very important that the file-names differ!
	 *
	 * @param origJar       original fat-JAR to embed the files into
	 * @param tempJarPrefix prefix for the new fat-JAR (temporary file)
	 * @param files         to be embedded
	 * @return path to new fat-JAR (a copy of the original) that contains the embedded artifacts
	 * @throws IOException
	 */
	public static Path embedFiles(File origJar, String tempJarPrefix, List<File> files) throws IOException {
		Map<String, String> env = Collections.singletonMap("create", "true");

		// Create temporary jar file
		Path copyPath = Files.createTempFile(tempJarPrefix, EXT_JAR);
		URI copyURI = URI.create(JAR_RESOURCE_PREFIX + copyPath.toUri());

		// Copy original jar into temporary
		Files.copy(origJar.toPath(), copyPath, StandardCopyOption.REPLACE_EXISTING);

		// Open temporary jar with FileSystem and place every file into the artifact folder
		try (FileSystem jarFs = FileSystems.newFileSystem(copyURI, env)) {
			Path artifactDir = jarFs.getPath(Paths.get("/").resolve(ARTIFACTS_FOLDER).toString());
			if (!Files.exists(artifactDir)) {
				Files.createDirectory(artifactDir);
			}

			for (File file : files) {
				Path filePath = file.toPath();
				Path pathInJar = artifactDir.resolve(file.getName());
				Files.copy(filePath, pathInJar, StandardCopyOption.REPLACE_EXISTING);
			}
		}

		return copyPath;
	}

	/**
	 * Serializes graph using Kryo
	 *
	 * @param tempFilePrefix prefix for the temporary file (will hold binary serialized graph)
	 * @param graph          to be serialized
	 * @return path to the file that contains the serialized graph
	 * @throws IOException
	 */
	public static Path serializeGraph(String tempFilePrefix, StreamGraph graph) throws IOException {
		Path graphPath = createTempFile(tempFilePrefix, EXT_STREAM_GRAPH_EXT);

		// Serialize object to file
		new KryoHandler().write(graphPath.toFile(), graph);

		return graphPath.toAbsolutePath();
	}

	/**
	 * Creates a temporary file and registers it for cleanup at shutdown by RapidMiner.
	 *
	 * @param prefix
	 * @param suffix
	 * @return path to newly created temporary file
	 * @throws IOException
	 */
	public static Path createTempFile(String prefix, String suffix) throws IOException {
		File tempFile = File.createTempFile(prefix, suffix);
		tempFile.deleteOnExit();
		return tempFile.toPath();
	}

}