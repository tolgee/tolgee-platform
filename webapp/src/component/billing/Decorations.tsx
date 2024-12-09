import { Box, Link, styled } from '@mui/material';

export const StyledBillingHint = styled(Box)`
  display: inline;
  text-decoration: underline;
  text-decoration-style: dashed;
  text-underline-offset: 0.2em;
  text-decoration-thickness: 1%;
`;

export const StyledBillingLink = styled(Link)`
  display: inline;
  text-decoration: underline;
  text-decoration-style: dashed;
  text-underline-offset: 0.2em;
  text-decoration-thickness: 1%;
  color: unset;
  &:hover,
  &:active {
    color: ${({ theme }) => theme.palette.primaryText};
  }
  transition: color 200ms ease-in-out;
`;
