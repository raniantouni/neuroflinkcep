import React from 'react';
import { Menu } from 'antd';

type WorkflowListProps = {
	workflows: Array<Workflow>;
	handlers: EventHandlers;
	selectedWorkflow?: Workflow;
};

export class WorkflowList extends React.Component<WorkflowListProps> {
	render(): JSX.Element {
		const { workflows, handlers, selectedWorkflow } = this.props;
		return (
			<Menu theme="dark" mode="inline" selectedKeys={selectedWorkflow ? [selectedWorkflow.id] : []}>
				{workflows.map((workflow) => (
					<Menu.Item key={workflow.id} onClick={(): void => handlers.onSelectWorkflow(workflow)}>
						{workflow.name}
					</Menu.Item>
				))}
			</Menu>
		);
	}
}
