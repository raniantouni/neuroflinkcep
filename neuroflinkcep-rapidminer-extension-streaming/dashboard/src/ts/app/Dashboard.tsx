import React from 'react';
import { Layout, Switch, Divider, Spin } from 'antd';
import { Footer } from 'antd/lib/layout/layout';

import {
	getWorkflows,
	stopWorkflow,
	stopJob,
	removeWorkflow,
	update,
	updateOptimizationJob,
	clearFinishedJobs,
	removeJob,
	openDeployedProcess
} from '../api/api_utils';
import { WorkflowDetail } from '../components/WorkflowDetail';
import { WorkflowList } from '../components/WorkflowList';
import RMBrand from '../../assets/Icon_Logo_FullColor_RapidMiner.png'

import classes from '../../stylesheets/Dashboard.module.less';

type DashboardState = {
	workflows: Array<Workflow>;
	selectedWorkflowId: string;
	autoRefresh: boolean;
};

// eslint-disable-next-line @typescript-eslint/ban-types
export class Dashboard extends React.Component<{}, DashboardState> {
	refreshInterval: number;
	refreshIntervalId?: NodeJS.Timeout;
	eventHandlers: EventHandlers;

	// eslint-disable-next-line @typescript-eslint/ban-types
	constructor(props: {}) {
		super(props);
		this.refreshInterval = 1000;
		this.refreshIntervalId = undefined;
		this.eventHandlers = {
			onSelectWorkflow: this.onSelectWorkflow,
			onStopWorkflow: this.onStopWorkflow,
			onStopJob: this.onStopJob,
			onRemoveJob: this.onRemoveJob,
			onRemoveWorkflow: this.onRemoveWorkflow,
			onClearFinishedJobs: this.onClearFinishedJobs,
			onUpdateOptimizationJob: this.onUpdateOptimizationWorkflow,
			onOpenDeployedProcess: this.onOpenDeployedProcess
		};
		this.state = {
			workflows: [],
			selectedWorkflowId: '',
			autoRefresh: false
		};
	}

	componentDidMount(): void {
		this.onRefresh();
	}

	componentWillUnmount(): void {
		this.setRefreshTimer(false);
	}

	setRefreshTimer = (on: boolean): void => {
		if (on) {
			console.debug('Setting timer');
			this.onRefresh();
			this.refreshIntervalId = setInterval(() => this.onRefresh(), this.refreshInterval);
		} else if (this.refreshIntervalId) {
			console.debug('Clearing timer');
			clearInterval(this.refreshIntervalId);
		}
	};

	getSelectedWorkflow(): Workflow | undefined {
		const { workflows, selectedWorkflowId } = this.state;
		return workflows.find((wf) => wf.id === selectedWorkflowId);
	}

	// eslint-disable-next-line @typescript-eslint/no-unused-vars
	onAutoRefresh = (checked: boolean, event: MouseEvent): void => {
		this.setState({ autoRefresh: checked });
		this.setRefreshTimer(checked);
	};

	onSelectWorkflow = (workflow: Workflow): void => {
		console.debug('Selected: ' + workflow.id);
		this.setState({
			selectedWorkflowId: workflow.id
		});
	};

	onStopJob = (workflow: Workflow, job: Job): void => {
		stopJob(workflow, job);
		this.updateWorkflows();
		this.onRefresh();
	};

	onRemoveJob = (workflow: Workflow, job: Job): void => {
		removeJob(workflow, job);
		this.updateWorkflows();
		this.onRefresh();
	};

	onStopWorkflow = (workflow: Workflow): void => {
		stopWorkflow(workflow);
		this.updateWorkflows();
		this.onRefresh();
	};

	onRemoveWorkflow = (workflow: Workflow): void => {
		removeWorkflow(workflow);
		this.updateWorkflows();
	};

	onClearFinishedJobs = (workflow: Workflow): void => {
		clearFinishedJobs(workflow);
		this.updateWorkflows();
	};

	onUpdateOptimizationWorkflow = (workflow: Workflow, job: Job): void => {
		updateOptimizationJob(workflow, job);
		this.updateWorkflows();
	}

	onOpenDeployedProcess = (workflow: Workflow, job: Job): void => {
		openDeployedProcess(workflow, job);
		this.updateWorkflows();
	}

	onRefresh = (): void => {
		update();
		setTimeout(() => this.updateWorkflows(), this.refreshInterval);
	};

	updateWorkflows = (): void => {
		const array = getWorkflows();
		const currentSelected = this.state.selectedWorkflowId;
		let newSelectedId = currentSelected;
		const notEmpty = array.length > 0;
		const shouldSelect = currentSelected === '' && notEmpty;
		const invalidSelect = currentSelected !== '' && notEmpty && !array.some((wf) => wf.id === currentSelected);

		// If no item is selected or the selected item is invalid and a valid can be selected
		if (shouldSelect || invalidSelect) {
			newSelectedId = array[0].id;
		}

		this.setState({
			workflows: array,
			selectedWorkflowId: newSelectedId
		});
	};

	render(): JSX.Element {
		const { Header, Content, Sider } = Layout;

		return (
			<Layout className={classes.layout}>
				<Header>
					<div className={classes.headerInner}>
						<span className={classes.brand}>
							<img src={RMBrand} className={classes.brandImage} alt="Streaming Dashboard" />
						</span>
						<div className={classes.navbar}>
							<span className={classes.logoContainer}>
								<span className={classes.productName}>Streaming Dashboard</span>
							</span>
						</div>
						<div className={classes.refreshSlider}>
							<Spin spinning={this.state.autoRefresh} />
							<Divider type="vertical" />
							Auto-refresh:
							<Divider type="vertical" />
							<Switch className="switcher" onChange={this.onAutoRefresh} />
						</div>
					</div>
				</Header>
				<Layout>
					<Sider theme="dark" width="25%" breakpoint="md" collapsedWidth="0">
						<WorkflowList
							selectedWorkflow={this.getSelectedWorkflow()}
							workflows={this.state.workflows}
							handlers={this.eventHandlers}
						/>
					</Sider>

					{/* Details dashboard (selected workflow + job details)*/}
					<Layout style={{ padding: '0 24px 24px' }}>
						<Content style={{ margin: '24px 16px 0' }}>
							<WorkflowDetail workflow={this.getSelectedWorkflow()} handlers={this.eventHandlers} />
						</Content>
					</Layout>
				</Layout>
				<Footer style={{ textAlign: 'center' }}>2022 RapidMiner Inc.</Footer>
			</Layout>
		);
	}
}
