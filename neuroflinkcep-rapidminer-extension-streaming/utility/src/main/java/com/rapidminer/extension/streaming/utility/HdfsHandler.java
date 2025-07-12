/*
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2019-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.utility;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DistributedFileSystem;

import com.google.common.collect.Maps;


/**
 * Utility for handling HDFS files
 *
 * @author Mate Torok
 * @since 0.1.0
 */
public class HdfsHandler {

	private final ConcurrentMap<String,DistributedFileSystem> activeTasks = Maps.newConcurrentMap();

	private final ExecutorService dfsExecutor = Executors.newFixedThreadPool(5);

	private final URI uri;

	private final Configuration configuration;

	public static final String HDFS_PATH_SEPARATOR = "/";

	public HdfsHandler(String uri, Configuration configuration) throws URISyntaxException {
		this.uri = new URI(uri);
		this.configuration = configuration;
	}

	/**
	 * Aborts/closes all operations
	 * @throws IOException in case something happens during abortion/closure
	 */
	public void abort() throws IOException {
		// Abort all operations
		for (DistributedFileSystem dfs : activeTasks.values()) {
			dfs.close();
		}
	}

	/**
	 * Copies local file to HDFS cluster (async)
	 *
	 * @param src local file path
	 * @param dst targeted HDFS path to copy to
	 * @return immutable pair of the TASK-ID and the Future for the asynchronous task (HDFS path for new file)
	 */
	public Future<String> copyLocalFile(File src, String dst) {
		return execute((dfs) -> {
			dfs.initialize(uri, configuration);

			Path srcPath = new Path(src.getPath());
			Path dstPath = new Path(dst);

			// Ensure target directory
			dfs.mkdirs(dstPath);

			// Copy from local path to HDFS
			dfs.copyFromLocalFile(srcPath, dstPath);

			return uri
				.resolve(dst)
				.resolve(src.getName())
				.toString();
		});
	}

	/**
	 * Copies file from HDFS cluster to local file (async)
	 *
	 * @param src source HDFS path to copy from
	 * @param dst local file path
	 * @return immutable pair of the TASK-ID and the Future for the asynchronous task (local path to new file)
	 */
	public Future<String> copyFileToLocal(String src, File dst) {
		return execute((dfs) -> {
			dfs.initialize(uri, configuration);
			dst.mkdirs();

			// Copy from HDFS to local path
			dfs.copyToLocalFile(new Path(src), new Path(dst.getPath()));

			return dst.getAbsolutePath();
		});
	}

	/**
	 * Created directory structure on HDFS cluster (async)
	 *
	 * @param dst HDFS path to create
	 * @return immutable pair of the TASK-ID and the Future for the asynchronous task (HDFS path to new directory)
	 */
	public Future<String> makeDirs(String dst) {
		return execute((dfs) -> {
			dfs.initialize(uri, configuration);
			dfs.mkdirs(new Path(dst));

			return uri.resolve(dst).toString();
		});
	}

	/**
	 * Executes the task asynchronously
	 * @param task to be executed (DFS is its input parameter, return value is a string/path)
	 * @return immutable pair of the TASK-ID and the Future for the asynchronous task
	 */
	private Future<String> execute(CheckedFunction<DistributedFileSystem,String> task) {
		// To register the DFS object with
		String taskId = UUID.randomUUID().toString();

		// Execute task asynchronously, result (string) will be returned as Future
		return dfsExecutor.submit(() -> {
			try (final DistributedFileSystem dfs = new DistributedFileSystem()) {
				activeTasks.put(taskId, dfs);
				return task.apply(dfs);
			} finally {
				activeTasks.remove(taskId);
			}
		});
	}

	@FunctionalInterface
	interface CheckedFunction<T, R> {

		R apply(T t) throws IOException;

	}

}