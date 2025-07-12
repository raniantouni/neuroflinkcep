import {State} from '../api/api.enum';

export const getStateColor = (state: State): string => {
	if (state === State.Unknown) {
		return 'grey';
	} else if (state === State.Stopping || state === State.DeployingNewPlan) {
		return 'yellow';
	} else if (state === State.Running || state === State.NewPlanAvailable) {
		return 'blue';
	} else if (state === State.Finished) {
		return 'green';
	} else {
		return 'red';
	}
};

export const getStateDescription = (state: State): string => {
	if (state === State.DeployingNewPlan) {
		return "Deploying new plan";
	} else if (state === State.Finished) {
		return "Finished";
	} else if (state === State.NewPlanAvailable) {
		return "New plan available";
	} else if (state === State.Running) {
		return "Running";
	} else if (state === State.Unknown) {
		return "Unknown";
	} else if (state === State.Stopping) {
		return "Stopping";
	} else if (state === State.Failed) {
		return "Failed";
	} else {
		return "Unknown";
	}
};
