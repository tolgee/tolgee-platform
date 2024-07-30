import { styled, useTheme } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { components } from 'tg.service/apiSchema.generated';

type TaskState = components['schemas']['TaskModel']['state'];

const StyledContainer = styled('span')`
  font-weight: 500;
`;

type Props = {
  state: TaskState;
};

export const TaskState = ({ state }: Props) => {
  const theme = useTheme();
  const { t } = useTranslate();

  const color =
    state === 'DONE'
      ? theme.palette.tokens._components.progressbar.task.done
      : theme.palette.tokens._components.progressbar.task.inProgress;

  const label =
    state === 'DONE' ? t('task_status_done') : t('task_status_in_progress');

  return <StyledContainer style={{ color }}>{label}</StyledContainer>;
};
