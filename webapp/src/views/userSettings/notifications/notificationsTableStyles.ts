import { Box, styled } from '@mui/material';

export const StyledSectionTitle = styled(Box)`
  font-size: 15px;
  font-weight: 500;
  text-transform: uppercase;
  align-self: end;
  line-height: 1.2;
`;

export const StyledColumnHeader = styled(Box)`
  align-self: end;
  justify-self: end;
  width: 34px;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  white-space: nowrap;
  line-height: 1.2;
  color: ${({ theme }) => theme.palette.text.secondary};
  font-size: 13px;
`;
