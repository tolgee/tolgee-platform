import { Link as MuiLink, styled } from '@mui/material';

export const StyledLink = styled(MuiLink)`
  text-transform: uppercase;
  font-size: 14px;
  cursor: pointer;
  &.disabled {
    color: ${({ theme }) => theme.palette.emphasis[500]};
    pointer-events: none;
  }
  &.secondary {
    color: ${({ theme }) => theme.palette.text.secondary};
  }
` as typeof MuiLink;
