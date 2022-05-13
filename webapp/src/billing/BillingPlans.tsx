import { components } from 'tg.service/billingApiSchema.generated';
import { Box } from '@mui/material';
import { FC } from 'react';
import { ActivePlan } from './ActivePlan';
import { Plan } from './Plan';

type BillingPlansProps = {
  plans: components['schemas']['PlanModel'][];
  activePlan: components['schemas']['ActivePlanModel'];
};

export const BillingPlans: FC<BillingPlansProps> = (props) => {
  return (
    <>
      {props.plans.map((plan) => (
        <Box key={plan.id}>
          {props.activePlan &&
            (props.activePlan.id === plan.id ? (
              <ActivePlan plan={props.activePlan} />
            ) : (
              <Plan
                plan={plan}
                isOrganizationSubscribed={!props.activePlan.free}
              />
            ))}
        </Box>
      ))}
    </>
  );
};
