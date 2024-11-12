import { Box, styled, SxProps } from '@mui/material';
import { Link } from 'react-router-dom';
import { components } from 'tg.service/apiSchema.generated';
import { getTaskRedirect } from './utils';

export const Container = styled(Box)`
  color: ${({ theme }) => theme.palette.tokens.icon.secondary};
  font-size: 15px;
`;

type SimpleProjectModel = components['schemas']['SimpleProjectModel'];

type TaskNumberProps = {
  sx?: SxProps;
  className?: string;
  taskNumber: number;
};

export const TaskNumber = ({ sx, className, taskNumber }: TaskNumberProps) => {
  return <Container {...{ sx, className }}>#{taskNumber}</Container>;
};

type TaskNumberWithLinkProps = {
  sx?: SxProps;
  className?: string;
  taskNumber: number;
  project: SimpleProjectModel;
};

export const TaskNumberWithLink = ({
  sx,
  className,
  taskNumber,
  project,
}: TaskNumberWithLinkProps) => {
  return (
    <Container
      component={Link}
      // @ts-ignore
      to={getTaskRedirect(project, taskNumber)}
      {...{ sx, className }}
    >
      #{taskNumber}
    </Container>
  );
};
