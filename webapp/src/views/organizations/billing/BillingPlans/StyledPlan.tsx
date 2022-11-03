import { styled } from '@mui/material';

export const StyledPlan = styled('div')`
  position: relative;
  background: ${({ theme }) => theme.palette.billingPlan.main};
  border: 1px solid ${({ theme }) => theme.palette.emphasis[200]};
  border-radius: 20px;
  overflow: hidden;
`;

export const StyledContent = styled('div')`
  padding: 20px;
  padding-top: 25px;
  display: grid;
  gap: 8px;
  grid-template-rows: 1fr auto auto;
  grid-template-areas:
    'title '
    'info  '
    'action';
`;

export const StyledSubtitle = styled('div')`
  position: absolute;
  top: 0px;
  left: 0px;
  right: 0px;
  font-size: 14px;
  padding: 0px 20px 0px 20px;
  color: ${({ theme }) => theme.palette.primary.main};
  background: ${({ theme }) =>
    theme.palette.mode === 'light' ? '#f3cfe0' : '#47333d'};
`;
