import { styled } from '@mui/material';

export const StyledShowMore = styled('button')`
  justify-self: center;
  margin: 8px 0 4px;
  border: none;
  background: none;
  padding: 0;
  font-family: inherit;
  font-size: 13px;
  font-weight: 500;
  letter-spacing: 0.46px;
  text-transform: uppercase;
  color: ${({ theme }) => theme.palette.text.secondary};
  cursor: pointer;

  &:hover {
    color: ${({ theme }) => theme.palette.text.primary};
  }
`;
