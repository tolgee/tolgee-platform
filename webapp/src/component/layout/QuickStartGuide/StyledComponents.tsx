import { Link as MuiLink, styled } from '@mui/material';

export const StyledLink = styled(MuiLink)`
  text-transform: uppercase;
  font-size: 14px;
  cursor: pointer;
  font-weight: 500;
  &.secondary {
    color: ${({ theme }) => theme.palette.text.secondary};
  }
  &.disabled {
    color: ${({ theme }) => theme.palette.emphasis[400]};
    pointer-events: none;
  }
` as typeof MuiLink;
