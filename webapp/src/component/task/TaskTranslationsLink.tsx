import React from 'react';
import { Link as RouterLink } from 'react-router-dom';

import { useBranchFromUrlPath } from 'tg.component/branching/useBranchFromUrlPath';
import { getTaskUrl } from 'tg.constants/links';
import { components } from 'tg.service/apiSchema.generated';
import { useBranchesService } from 'tg.views/projects/translations/context/services/useBranchesService';
import { TASK_ACTIVE_STATES } from './taskActiveStates';

type TaskModel = components['schemas']['TaskModel'];

type Props<T extends React.ElementType> = {
  task: TaskModel;
  projectId?: number;
  component?: T;
} & Omit<React.ComponentPropsWithoutRef<T>, 'component' | 'to'>;

export const TaskTranslationsLink = <
  T extends React.ElementType = typeof RouterLink
>({
  task,
  projectId,
  component,
  ...rest
}: Props<T>) => {
  const { selectedName } = useBranchesService({
    projectId,
    branchName: useBranchFromUrlPath(),
  });
  const allowLink = !(
    task.branchName &&
    task.branchName != selectedName &&
    !TASK_ACTIVE_STATES.includes(task.state)
  );
  const to = projectId && allowLink ? getTaskUrl(projectId, task.number) : '';
  const Component = (component ?? RouterLink) as React.ElementType;

  if (!to) {
    const Fallback = Component === RouterLink ? 'span' : Component;
    const style = { cursor: 'default', ...(rest as any).style };
    return <Fallback {...rest} style={style} />;
  }

  if (Component === RouterLink) {
    return <Component to={to} {...rest} />;
  }

  return <Component component={RouterLink} to={to} {...rest} />;
};
