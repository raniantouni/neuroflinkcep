export enum State {
	Unknown = 'Unknown',
	Running = 'Running',
	Stopping = 'Stopping',
	Failed = 'Failed',
	Finished = 'Finished',
	NewPlanAvailable = 'NewPlanAvailable',
	DeployingNewPlan = 'DeployingNewPlan'
}

export enum JobType {
	Flink = 'Flink',
	Spark = 'Spark',
	InforeOptimizer = 'InforeOptimizer'
}
