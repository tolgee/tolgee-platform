import { components } from 'tg.service/billingApiSchema.generated';
import { Box, Switch } from '@mui/material';
import { FC, useState } from 'react';
import { ActivePlan } from './ActivePlan';
import { Plan } from './Plan';

type BillingPlansProps = {
  plans: components['schemas']['PlanModel'][];
  activePlan: components['schemas']['ActivePlanModel'];
};

export const BillingPlans: FC<BillingPlansProps> = (props) => {
  const [billedYearly, setBilledYearly] = useState(false);
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

      {props.plans.map((plan) => (
        <Box key={plan.id}>
          {props.activePlan &&
            (props.activePlan.id === plan.id ? (
              <ActivePlan plan={props.activePlan} period={period} />
            ) : (
              <Plan
                plan={plan}
                isOrganizationSubscribed={!props.activePlan.free}
                period={period}
              />
            ))}
        </Box>
      ))}
    </>
  );
};
