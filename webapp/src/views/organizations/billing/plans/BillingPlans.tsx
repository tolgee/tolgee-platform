import { components } from 'tg.service/billingApiSchema.generated';
import { Box, styled, Switch } from '@mui/material';
import { FC, useRef, useState } from 'react';
import { Plan } from './Plan';

const StyledPlans = styled('div')`
  display: grid;
  grid-auto-flow: column;
  grid-auto-columns: minmax(250px, 300px);
  overflow-y: auto;
  gap: 32px;
`;

const StyledPlanWrapper = styled('div')``;

type BillingPlansProps = {
  plans: components['schemas']['PlanModel'][];
  activePlan: components['schemas']['ActivePlanModel'];
};

export const BillingPlans: FC<BillingPlansProps> = ({ plans, activePlan }) => {
  const [billedYearly, setBilledYearly] = useState(false);
  const plansRef = useRef<HTMLDivElement>(null);
  const period = billedYearly ? 'YEARLY' : 'MONTHLY';

  return (
    <>
      <Box>
        Monthly{' '}
        <Switch
          checked={billedYearly}
          onChange={(_, val) => setBilledYearly(val)}
        />{' '}
        Yearly
      </Box>

      <StyledPlans ref={plansRef}>
        {plans.map((plan) => (
          <StyledPlanWrapper key={plan.id}>
            {activePlan && (
              <>
                <Plan
                  plan={plan}
                  activePlan={
                    activePlan.id === plan.id ? activePlan : undefined
                  }
                  isOrganizationSubscribed={!activePlan.free}
                  period={period}
                />
              </>
            )}
          </StyledPlanWrapper>
        ))}
      </StyledPlans>
    </>
  );
};
