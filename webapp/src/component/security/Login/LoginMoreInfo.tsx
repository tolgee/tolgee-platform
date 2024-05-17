import { styled, useMediaQuery } from '@mui/material';
import { MouseIllustration } from '../MouseIllustration';
import { TolgeeMore } from '../TolgeeMore';
import { SPLIT_CONTENT_BREAK_POINT } from 'tg.component/layout/CompactView';

const StyledContainer = styled('div')`
  display: grid;
  grid-template-rows: 1fr auto;
  gap: 40px;
  align-self: end;
`;

export const LoginMoreInfo = () => {
  const isSmall = useMediaQuery(SPLIT_CONTENT_BREAK_POINT);
  return (
    <StyledContainer>
      {!isSmall && <MouseIllustration />}
      <TolgeeMore />
    </StyledContainer>
  );
};
