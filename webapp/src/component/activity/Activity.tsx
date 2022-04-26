import { styled } from '@mui/material';
import { components } from 'tg.service/apiSchema.generated';
import { ActivityConfigurable } from './ActivityConfigurable';

const StyledContainer = styled('div')`
  display: grid;
`;

type ProjectActivityModel = components['schemas']['ProjectActivityModel'];

type Props = {
  data: ProjectActivityModel;
};

export const Activity: React.FC<Props> = ({ data }) => {
  return (
    <StyledContainer>
      <ActivityConfigurable data={data} />
    </StyledContainer>
  );
};
