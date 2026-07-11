import { Box, Typography, InputBase, styled, MenuItem } from '@mui/material';

export const StyledInput = styled(InputBase)`
  padding: 0px 4px 0px 16px;
  flex-grow: 1;
  font-size: 16px;
`;

export const StyledInputWrapper = styled(Box)`
  display: flex;
  align-items: center;
  border-bottom: 1px solid ${({ theme }) => theme.palette.divider1};
  padding: 2px 4px 2px 0px;
  position: relative;
`;

export const StyledHeading = styled(Typography)`
  display: flex;
  flex-grow: 1;
  padding: 4px 4px 4px 16px;
  font-weight: 500;
`;

export const StyledInputContent = styled('div')`
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  height: 23px;
  width: 100%;
`;

export const StyledWrapper = styled('div')`
  display: grid;
`;

export const StyledCompactMenuItem = styled(MenuItem)`
  height: 40px;
`;
