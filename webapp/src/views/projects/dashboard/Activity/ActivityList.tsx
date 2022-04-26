import { styled } from '@mui/material';
import { components } from 'tg.service/apiSchema.generated';
import { ActivityItem } from './ActivityItem';

type ProjectActivityModel = components['schemas']['ProjectActivityModel'];

const StyledContainer = styled('div')`
  display: grid;
  gap: 16px;
`;

type Props = {
  data?: ProjectActivityModel[];
};

export const ActivityList: React.FC<Props> = ({ data }) => {
  return (
    <StyledContainer>
      {data?.map((item) => (
        <ActivityItem key={item.revisionId} item={item} />
      ))}
    </StyledContainer>
  );
};
