import { Box, styled } from '@mui/material';

export const KeyPanel = styled(Box)`
  flex: 1;
  border: 1px solid ${({ theme }) => theme.palette.tokens.border.primary};
  border-radius: ${({ theme }) => theme.spacing(1)};
  display: flex;
  flex-direction: column;

  &.accepted {
    border-color: ${({ theme }) => theme.palette.tokens.success.main};
  }
`;

export const KeyHeader = styled(Box)`
  display: grid;
  grid-template-columns: 1fr auto;
  background: ${({ theme }) => theme.palette.tokens.background.hover};
  padding: ${({ theme }) => theme.spacing(1.5, 2)};

  &.accepted {
    background: ${({ theme }) => theme.palette.tokens.success._states.selected};
  }
`;

export const AcceptButton = styled(Box)`
  display: flex;
  align-items: center;
`;

export const TranslationList = styled(Box)`
  display: grid;
`;

export const StyledLanguageField = styled('div')`
  border-color: ${({ theme }) => theme.palette.divider1};
  border-width: 1px 1px 1px 0;
  border-style: solid;

  & + & {
    border-top: 0;
  }
`;

export const KeyFooter = styled(Box)`
  display: flex;
  justify-content: center;
  padding: ${({ theme }) => theme.spacing(1, 2)};
  border-top: 1px solid ${({ theme }) => theme.palette.divider1};
`;
