import React, { ReactNode } from 'react';
import { PageHeader, Button, Tag, Row, Col, Statistic } from 'antd';

import { State } from '../api/api.enum';
import {getStateColor, getStateDescription} from '../utils/uiUtils';
import { openProcess } from '../api/api_utils';
import { JobItem } from './JobItem';

type WorkflowDetailProps = {
	workflow?: Workflow;
	handlers: EventHandlers;
};

export class WorkflowDetail extends React.Component<WorkflowDetailProps> {
	render(): JSX.Element {
		const { workflow, handlers } = this.props;
		return (
			<div>
				{workflow ? (
					<div>
						<Row gutter={[24, 8]}>
							<Col span={24}>
								<PageHeader
									className="site-page-header"
									title="Workflow Details"
									extra={[
										<Button
											key="1"
											onClick={(): void => handlers.onStopWorkflow(workflow)}
											hidden={workflow.state !== State.Running && workflow.state !== State.NewPlanAvailable}
											type="primary"
											shape="round"
											size="small">
											Stop workflow
										</Button>,
										<Button
												key="2"
												danger
												onClick={(): void => handlers.onClearFinishedJobs(workflow)}
												type="default"
												shape="round"
												size="small">
											Remove finished jobs
										</Button>,
										<Button
											key="3"
											danger
											onClick={(): void => handlers.onRemoveWorkflow(workflow)}
											type="default"
											shape="round"
											size="small">
											Remove
										</Button>
									]}
								/>
							</Col>
						</Row>
						<Row gutter={[24, 48]}>
							<Col>
								<Statistic title="Worfklow name" value={workflow.name} valueStyle={{ fontSize: 18 }} />
							</Col>
							<Col>
								<Statistic
									title="Worfklow status"
									valueRender={(): React.ReactNode => (
										<Tag color={getStateColor(workflow.state)}>{getStateDescription(workflow.state)}</Tag>
									)}
								/>
							</Col>
							<Col>
								<Statistic
									title="Process location"
									formatter={(): ReactNode => (
										<Button type="link" onClick={(): void => openProcess(workflow.processLocation)}>
											{workflow.processLocation}
										</Button>
									)}
								/>
							</Col>
							<Col>
								<Statistic
									title="Start time"
									value={workflow.startTime}
									valueStyle={{ fontSize: 18 }}
								/>
							</Col>
						</Row>
						<Row gutter={[24, 8]}>
							<Col span={24}>
								<PageHeader title="Jobs" />
							</Col>
						</Row>
						<Row hidden={workflow.jobs.length == 0} gutter={[24, 8]}>
							{workflow.jobs.map((job) => (
								<Col key={job.id}>
									<JobItem key={job.id} workflow={workflow} job={job} handlers={handlers} />
								</Col>
							))}
						</Row>
					</div>
				) : (
					<div>
						<Row gutter={[24, 8]}>
							<Col span={24}>
								<PageHeader className="site-page-header" title="No workflow selected..." />
							</Col>
						</Row>
					</div>
				)}
			</div>
		);
	}
}
