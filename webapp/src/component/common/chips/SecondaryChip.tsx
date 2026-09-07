import { styled } from '@mui/material';
import { BaseChip } from 'tg.component/common/chips/DefaultChip';

export const SecondaryChip = styled(BaseChip)`
  background-color: ${({ theme }) =>
    theme.palette.tokens.secondary._states.selected};
  color: ${({ theme }) => theme.palette.tokens.secondary.main};

  & .MuiChip-icon {
    color: ${({ theme }) => theme.palette.tokens.secondary.main};
    fill: ${({ theme }) => theme.palette.tokens.secondary.main};
  }

  & .MuiChip-label {
    font-weight: 600;
  }
`;
