import { styled } from '@mui/material';
import { DefaultChip } from 'tg.component/common/chips/DefaultChip';

export const TransparentChip = styled(DefaultChip)`
  background-color: ${({ theme }) => theme.palette.tokens.background.onDefault};
  border: 1px solid
    ${({ theme }) =>
      theme.palette.tokens._components.input.outlined.enabledBorder};
`;
