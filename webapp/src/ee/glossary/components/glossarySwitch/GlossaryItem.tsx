import { Box, styled } from '@mui/material';
import { components } from 'tg.service/apiSchema.generated';

type SimpleGlossaryModel = components['schemas']['SimpleGlossaryModel'];

const StyledWrapper = styled(Box)`
  display: flex;
  align-items: center;
  gap: 6px;
`;

type Props = {
  data: SimpleGlossaryModel;
};

export const GlossaryItem: React.FC<Props> = ({ data }) => {
  return (
    <StyledWrapper>
      <Box>{data.name}</Box>
    </StyledWrapper>
  );
};
