type Job = {
	id: string;
	name: string;
	state: State;
	type: JobType;
	remoteDashboardURL: string;
	optimizerResponseInfo: string;
};

type Workflow = {
	id: string;
	name: string;
	state: State;
	processLocation: string;
	startTime: string;
	jobs: Array<Job>;
};
