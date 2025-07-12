declare global {
	interface Window {
		rm_api: {
			openProcess: (location: string) => void;
			update: () => void;
			getWorkflows: () => string;
			stopWorkflow: (id: string) => void;
			stopJob: (wId: string, jId: string) => void;
			removeJob: (wId: string, jId: string) => void;
			removeWorkflow: (id: string) => void;
			clearFinishedJobs : (id: string) => void;
			updateOptimizationJob: (wId: string, jId: string) => void;
			openDeployedProcess: (wId: string, jId: string) => void;
		};
	}
}

export const openProcess = (location: string): void => {
	console.debug('Opening process: ' + location);
	return window.rm_api.openProcess(location);
};

export const update: () => void = function () {
	console.debug('Updating data-store');
	return window.rm_api.update();
};

export const getWorkflows = (): Array<Workflow> => {
	console.debug('Getting workflows from API');
	return JSON.parse(window.rm_api.getWorkflows());
};

export const stopWorkflow = (workflow: Workflow): void => {
	console.debug('Stopping workflow via API: ' + workflow.id);
	return window.rm_api.stopWorkflow(workflow.id);
};

export const removeWorkflow = (workflow: Workflow): void => {
	console.debug('Removing workflow via API: ' + workflow.id);
	return window.rm_api.removeWorkflow(workflow.id);
};

export const stopJob = (workflow: Workflow, job: Job): void => {
	console.debug('Stopping job for worfklow via API: ' + workflow.id + ', job: ' + job.id);
	return window.rm_api.stopJob(workflow.id, job.id);
};

export const removeJob = (workflow: Workflow, job: Job): void => {
	console.debug('Removing job for worfklow via API: ' + workflow.id + ', job: ' + job.id);
	return window.rm_api.removeJob(workflow.id, job.id);
};

export const clearFinishedJobs = (workflow: Workflow): void => {
	console.debug('Clearing finished jobs from workflow via API: ' + workflow.id);
	return window.rm_api.clearFinishedJobs(workflow.id);
};

export const updateOptimizationJob = (workflow: Workflow, job: Job): void => {
	console.debug('Update Optimization job via API: ' + workflow.id + ', job: ' + job.id);
	return window.rm_api.updateOptimizationJob(workflow.id, job.id);
};

export const openDeployedProcess = (workflow: Workflow, job: Job): void => {
	console.debug('Open deployed process: ' + workflow.id + ', job: ' + job.id);
	return window.rm_api.openDeployedProcess(workflow.id, job.id);
};
