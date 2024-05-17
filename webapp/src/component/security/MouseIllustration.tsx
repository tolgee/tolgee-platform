import { Box, styled, useTheme } from '@mui/material';

const StyledIllustration = styled(Box)`
  display: grid;
  align-content: center;
  justify-content: center;
`;

const StyledMouse = styled('img')`
  user-select: none;
  pointer-events: none;
`;

export function MouseIllustration() {
  const theme = useTheme();
  return (
    <StyledIllustration>
      <StyledMouse
        src={
          theme.palette.mode === 'dark'
            ? '/images/mouseYogaDark.svg'
            : '/images/mouseYogaLight.svg'
        }
      />
    </StyledIllustration>
  );
}
