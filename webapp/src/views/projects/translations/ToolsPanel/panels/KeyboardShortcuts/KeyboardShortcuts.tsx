import { Box, styled } from '@mui/material';
import { TranslationsShortcuts } from 'tg.component/shortcuts/TranslationsShortcuts';

const StyledContainer = styled(Box)`
  height: 100%;
  margin: 0px 8px;
  display: flex;
`;

export const KeyboardShortcuts = () => {
  return (
    <StyledContainer>
      <TranslationsShortcuts />
    </StyledContainer>
  );
};
