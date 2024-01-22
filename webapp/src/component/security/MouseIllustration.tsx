import { Box, styled } from '@mui/material';
import ComeIn from 'tg.svgs/signup/comeIn.svg?react';

const StyledIllustration = styled(Box)`
  display: grid;
  position: relative;
  margin-bottom: 100px;
`;

const StyledComeIn = styled(ComeIn)`
  color: ${({ theme }) => theme.palette.text.secondary};
`;

const StyledMouse = styled('img')`
  position: absolute;
  bottom: -25px;
  right: -130px;
  user-select: none;
  pointer-events: none;
`;

export function MouseIllustration() {
  return (
    <StyledIllustration>
      <Box display="flex" flexGrow="1" alignItems="end" justifyContent="center">
        <StyledComeIn />
      </Box>
      <StyledMouse src="/images/standardMouse.svg" />
    </StyledIllustration>
  );
}
