import { Box, styled, SxProps } from '@mui/material';
import { components } from 'tg.service/apiSchema.generated';
import { TaskTranslationsLink } from 'tg.component/task/TaskTranslationsLink';

const containerStyles = `
  color: ${({ theme }) => theme.palette.tokens.icon.secondary};
  font-size: 15px;
`;

export const Container = styled(Box)`
  ${containerStyles}
`;

export const LinkContainer = styled(Box)`
  ${containerStyles}
`;

type SimpleProjectModel = components['schemas']['SimpleProjectModel'];
type TaskModel = components['schemas']['TaskModel'];

type TaskNumberProps = {
  sx?: SxProps;
  className?: string;
  taskNumber: number;
};

export const TaskNumber = ({ sx, className, taskNumber }: TaskNumberProps) => {
  return (
    <Container data-cy="task-number" {...{ sx, className }}>
      #{taskNumber}
    </Container>
  );
};

type TaskNumberWithLinkProps = {
  sx?: SxProps;
  className?: string;
  task: TaskModel;
  project: SimpleProjectModel;
};

export const TaskNumberWithLink = ({
  sx,
  className,
  task,
  project,
}: TaskNumberWithLinkProps) => {
  return (
    <TaskTranslationsLink
      component={LinkContainer}
      task={task}
      projectId={project.id}
      {...{ sx, className }}
    >
      #{task}
    </TaskTranslationsLink>
  );
};
