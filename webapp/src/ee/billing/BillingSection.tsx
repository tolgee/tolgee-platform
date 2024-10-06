import { Box, styled } from '@mui/material';

export const StyledBillingSection = styled(Box)`
  display: grid;
`;

export const StyledBillingSectionTitle = styled('div')`
  font-size: 24px;
`;

export const StyledBillingSectionSubtitle = styled('div')`
  font-size: 24px;
  color: ${({ theme }) => theme.palette.primary.main};
`;

export const StyledBillingSectionSubtitleSmall = styled('span')`
  font-size: 14px;
  color: ${({ theme }) => theme.palette.primary.main};
`;

export const StyledBillingSectionHeader = styled('div')`
  display: flex;
  justify-content: space-between;
  gap: 10px;
  flex-wrap: wrap;
  align-items: center;
`;
