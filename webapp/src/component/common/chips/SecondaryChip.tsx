import { styled } from '@mui/material';
import { BaseChip } from 'tg.component/common/chips/DefaultChip';

/**
 * Teal accent chip, on the same token pair as the active-plan type tag and PlanSubtitle. For
 * things worth noticing rather than plain metadata — a bonus, a highlight.
 */
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
