import { Box, styled } from '@mui/material';
import { TranslationsShortcuts } from 'tg.component/shortcuts/TranslationsShortcuts';

const StyledContainer = styled(Box)`
  height: 100%;
  margin: 0px 8px;
  display: grid;
`;

export const KeyboardShortcuts = () => {
  return (
    <StyledContainer>
      <TranslationsShortcuts />
    </StyledContainer>
  );
};
