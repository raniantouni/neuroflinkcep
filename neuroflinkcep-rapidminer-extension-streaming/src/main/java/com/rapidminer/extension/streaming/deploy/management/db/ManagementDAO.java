/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.deploy.management.db;

import static com.rapidminer.extension.streaming.PluginInitStreaming.getWorkspace;
import static java.util.Optional.of;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.io.File;
import java.nio.charset.Charset;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;

import com.rapidminer.extension.streaming.deploy.management.api.Status;
import com.rapidminer.extension.streaming.utility.JsonUtil;
import com.rapidminer.tools.LogService;


/**
 * Data store backed by a JSON document to store workflows (persistently) for later management.
 * The accessor takes care of "atomic" access.
 *
 * @author Mate Torok
 * @since 0.2.0
 */
public class ManagementDAO {

	private static final Logger LOGGER = LogService.getRoot();

	private static final Object LOCK = new Object();

	private static final String DATA_FILE = getWorkspace().resolve("streaming_workflow_db.json").toString();

	private static ManagementDB managementDB;

	static {
		try {
			FileUtils.touch(new File(DATA_FILE));
			readDB();
		} catch (Throwable e) {
			LOGGER.log(Level.WARNING, "Might not be able to persist streaming workflow data", e);
		}
	}

	/**
	 * Ensures synchronised access to the workflows
	 * @return workflows from the "DB" (sorted in descending/reversed order according to "startTime", ~last first)
	 */
	public static List<Workflow> getWorkflows() {
		synchronized (LOCK) {
			return managementDB.getWorkflows()
				.stream()
				.sorted(Comparator.comparingLong(Workflow::getStartTime).reversed())
				.collect(Collectors.toList());
		}
	}

	/**
	 * Ensures synchronised access to the workflow
	 * @param id
	 * @return workflow from the "DB"
	 */
	public static Workflow getWorkflow(String id) {
		synchronized (LOCK) {
			return managementDB.getWorkflow(id);
		}
	}

	/**
	 * Ensures synchronised removal of a workflow
	 * @param workflowId
	 */
	public static void removeWorkflow(String workflowId) {
		synchronized (LOCK) {
			managementDB.removeWorkflow(workflowId);
			writeDB();
		}
	}

	/**
	 * Ensures synchronised addition/update of a workflow
	 * @param workflow
	 */
	public static void addOrUpdate(Workflow workflow) {
		synchronized (LOCK) {
			managementDB.addOrUpdate(workflow);
			writeDB();
		}
	}

	/**
	 * Ensures synchronised addition/update of a job
	 * @param workflowId
	 * @param job
	 */
	public static void addOrUpdate(String workflowId, Job job) {
		synchronized (LOCK) {
			managementDB.addOrUpdate(workflowId, job);
			writeDB();
		}
	}

	/**
	 * Ensures synchronised removal of a job
	 * @param workflowId
	 */
	public static void removeJob(String workflowId, Job job) {
		synchronized (LOCK) {
			managementDB.removeJob(workflowId, job);
			writeDB();
		}
	}

	/**
	 * Ensures synchronised state update of a job, if its current state matches "oldStat"
	 * @param workflowId
	 * @param jobId
	 * @param oldStat
	 * @param newStat
	 */
	public static void updateState(String workflowId, String jobId, Status oldStat, Status newStat) {
		synchronized (LOCK) {
			of(managementDB)
				.map(db -> db.getWorkflow(workflowId))
				.map(workflow -> workflow.getJobs().get(jobId))
				.filter(job -> job.getState() == oldStat)
				.ifPresent(job -> {
					managementDB.addOrUpdate(
						workflowId,
						new Job(
							job.getWorkflowId(),
							job.getUniqueId(),
							job.getRemoteId(),
							job.getEndpoint(),
							job.getName(),
							job.getType(),
							job.isMonitorable(),
							newStat,
							job.getRemoteDashboardURL()));
					writeDB();
				});
		}
	}

	/**
	 * Reads and deserializes the "DB" from the filesystem
	 */
	private static void readDB() {
		try {
			File input = new File(DATA_FILE);
			LOGGER.fine("Reading DB");
			String readData = FileUtils.readFileToString(input, Charset.defaultCharset());
			if (isBlank(readData)) {
				managementDB = new ManagementDB();
			} else {
				managementDB = JsonUtil.fromString(readData, ManagementDB.class);
			}
		} catch (Throwable e) {
			LOGGER.log(Level.WARNING, "Could not successfully read the data-base", e);
			managementDB = new ManagementDB();
		}
	}

	/**
	 * Serializes and writes the "DB" onto the disk/filesystem (~persistence)
	 */
	private static void writeDB() {
		File output = new File(DATA_FILE);
		String db = JsonUtil.toJson(managementDB);
		LOGGER.fine("Writing DB");

		// Writing the file from workers/"main" thread
		AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
			try {
				FileUtils.write(output, db, Charset.defaultCharset());
			} catch(Throwable e) {
				LOGGER.log(Level.WARNING, "Could not successfully write the data-base", e);
			}
			return null;
		});
	}

}