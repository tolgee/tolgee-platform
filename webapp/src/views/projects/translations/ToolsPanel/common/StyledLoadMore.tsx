import { styled } from '@mui/material';

export const StyledLoadMore = styled('div')`
  display: flex;
  justify-content: center;
  align-items: flex-end;
  z-index: 2;
  padding-top: 8px;
  padding-bottom: 2px;
`;

export const StyledLoadMoreButton = styled('div')`
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  text-transform: uppercase;
  color: ${({ theme }) => theme.palette.text.secondary};
`;
