import { styled } from '@mui/material';

export const Plan = styled('div')`
  display: grid;
  position: relative;
  background: ${({ theme }) => theme.palette.billingPlan.main};
  border: 1px solid ${({ theme }) => theme.palette.emphasis[200]};
  border-radius: 20px;
  overflow: hidden;
`;

export const PlanContent = styled('div')`
  padding: 25px 20px 20px;
  display: grid;
  gap: 8px;
  grid-template-rows: auto 1fr auto;
  grid-template-areas:
    'title estimated-costs'
    'info  info'
    'period-switch period-switch'
    'price action';
  height: 100%;
`;

export const PlanSubtitle = styled('div')`
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

export const PlanEstimatedCostsArea = styled('div')`
  grid-area: estimated-costs;
`;

export const PlanEstimatedCosts = styled(PlanEstimatedCostsArea)`
  justify-self: end;
`;
