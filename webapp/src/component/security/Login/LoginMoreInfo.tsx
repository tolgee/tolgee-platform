import { styled, useMediaQuery } from '@mui/material';
import { MouseIllustration } from '../MouseIllustration';
import { SPLIT_CONTENT_BREAK_POINT } from '../SplitContent';
import { TolgeeMore } from '../TolgeeMore';

const StyledContainer = styled('div')`
  display: flex;
  flex-direction: column;
  align-items: stretch;
  justify-content: space-between;
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
