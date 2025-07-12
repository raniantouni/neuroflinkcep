type EventHandlers = {
	onRemoveWorkflow: (workflow: Workflow) => void;
	onSelectWorkflow: (workflow: Workflow) => void;
	onStopWorkflow: (workflow: Workflow) => void;
	onStopJob: (workflow: Workflow, job: Job) => void;
	onRemoveJob: (workflow: Workflow, job: Job) => void;
	onClearFinishedJobs: (workflow: Workflow) => void;
	onUpdateOptimizationJob: (workflow: Workflow, job: Job) => void;
	onOpenDeployedProcess: (workflow: Workflow, job: Job) => void;
};
