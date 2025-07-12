import React from 'react';
import { Card, Button, Statistic, Tag, Row, Col } from 'antd';

import {JobType, State} from '../api/api.enum';
import {getStateColor, getStateDescription} from '../utils/uiUtils';

type JobItemProps = {
	workflow: Workflow;
	job: Job;
	handlers: EventHandlers;
};

export class JobItem extends React.Component<JobItemProps> {
	render(): JSX.Element {
		const { workflow, job, handlers } = this.props;
		return (
			<Card
				title={job.name}
				style={{ minWidth: 300 }}
				extra={[
					<Button
						key="1"
						onClick={(): void => handlers.onStopJob(workflow, job)}
						hidden={job.state !== State.Running && job.state !== State.NewPlanAvailable}
						type="primary"
						shape="round"
						size="small">
						Stop job
					</Button>,
					<Button
						key="2"
						onClick={(): void => handlers.onRemoveJob(workflow, job)}
						hidden={job.state !== State.Finished && job.state !== State.Failed}
						type="primary"
						shape="round"
						size="small">
						Remove job
					</Button>
				]}>
				<Row gutter={[48, 8]}>
					<Col>
						<Statistic title="Type" value={job.type} />
					</Col>
					<Col>
						<Statistic
							title="Status"
							valueRender={(): React.ReactNode => <Tag color={getStateColor(job.state)}>{getStateDescription(job.state)}</Tag>}
						/>
					</Col>
				</Row>
				<Button
					key="1"
					target="_blank"
					hidden={job.remoteDashboardURL === '' || job.remoteDashboardURL == null}
					href={job.remoteDashboardURL}
					type="link">
					Go to remote dashboard
				</Button>
				<Button
						key="2"
						hidden={job.type !== JobType.InforeOptimizer}
						onClick={(): void => handlers.onOpenDeployedProcess(workflow, job)}
						type="primary"
						shape="round"
						size="small">
					Open deployed process
				</Button>
				<Button
					key="3"
					hidden={job.state !== State.NewPlanAvailable || job.optimizerResponseInfo === '' || job.optimizerResponseInfo == null}
					onClick={(): void => handlers.onUpdateOptimizationJob(workflow, job)}
					type="primary"
					shape="round"
					size="small">
					{job.optimizerResponseInfo}
				</Button>
			</Card>
		);
	}
}
