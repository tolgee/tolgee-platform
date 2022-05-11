import { styled, Box } from '@mui/material';

import { ActivityUser } from '../ActivityUser';
import { ActivityEntities } from './ActivityEntities';
import { Activity, ActivityModel } from '../types';

const StyledContainer = styled('div')`
  display: grid;
  gap: 3px;
  & > * {
    overflow: hidden;
  }

  & .showOnMouseOver {
    opacity: 0;
    pointer-events: none;
    transition: opacity 0.1s ease-out;
  }

  &:hover .showOnMouseOver {
    opacity: 1;
    pointer-events: all;
    transition: opacity 0.3s ease-in;
  }
`;

const StyledUserWrapper = styled(Box)`
  display: flex;
`;

type Props = {
  data: ActivityModel;
  activity: Activity;
  diffEnabled: boolean;
};

export const ActivityDetail = ({ data, activity, diffEnabled }: Props) => {
  return (
    <StyledContainer data-cy="activity-detail">
      <StyledUserWrapper>
        <ActivityUser item={data} />
      </StyledUserWrapper>
      <ActivityEntities activity={activity} diffEnabled={diffEnabled} />
    </StyledContainer>
  );
};
