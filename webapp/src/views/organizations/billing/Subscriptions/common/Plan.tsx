import { Box, styled } from '@mui/material';

export const Plan = styled('div')`
  display: grid;
  position: relative;
  border: 1px solid ${({ theme }) => theme.palette.background.paper};
  background: ${({ theme }) => theme.palette.background.paper};
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0px 0px 20px 0px
    ${({ theme }) => theme.palette.billingPlan.shadow};
  background: ${({ theme }) => theme.palette.billingPlan.background};

  &.active {
    box-shadow: 0px 0px 20px 0px
      ${({ theme }) => theme.palette.billingPlan.activeShadow};
    border-color: ${({ theme }) => theme.palette.billingPlan.activeBorder};
  }
`;

export const PlanContent = styled('div')`
  padding: 32px 24px;
  display: grid;
  grid-template-rows: auto 1fr;
  height: 100%;
`;

export const PlanTitle = styled('div')`
  justify-self: center;
  font-size: 24px;
  color: ${({ theme }) => theme.palette.primary.main};
`;

export const PlanSubtitle = styled('div')`
  position: absolute;
  top: 0px;
  left: 0px;
  right: 0px;
  font-size: 18px;
  text-align: center;
  font-weight: 500;
  color: ${({ theme }) => theme.palette.billingPlan.subtitleText};
  background: ${({ theme }) => theme.palette.billingPlan.subtitleBg};
`;

export const PlanEstimatedCostsArea = styled('div')`
  /* grid-area: estimated-costs; */
`;

export const PlanEstimatedCosts = styled(PlanEstimatedCostsArea)`
  justify-self: end;
`;

export const PlanFeaturesBox = styled(Box)`
  display: grid;
  border-radius: 20px;
  padding: 24px 20px;
  background: ${({ theme }) => theme.palette.billingPlan.featuresBg};
  align-self: start;
`;
