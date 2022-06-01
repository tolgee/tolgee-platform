import { FC } from 'react';
import { Box } from '@mui/material';
import { useTranslate } from '@tolgee/react';

import LoadingButton from 'tg.component/common/form/LoadingButton';
import { components } from 'tg.service/billingApiSchema.generated';
import { PlanInfo } from './PlanInfo';
import { usePlan } from './usePlan';
import { ActivePlanInfo } from './ActivePlanInfo';

type PlanModel = components['schemas']['PlanModel'];
type ActivePlanModel = components['schemas']['ActivePlanModel'];
type Period = components['schemas']['SubscribeRequest']['period'];

type Props = {
  plan: PlanModel;
  activePlan?: ActivePlanModel;
  isOrganizationSubscribed: boolean;
  period: Period;
};

export const Plan: FC<Props> = ({
  plan,
  activePlan,
  isOrganizationSubscribed,
  period,
}) => {
  const t = useTranslate();

  const {
    onUpgrade,
    upgradeMutation,
    onSubscribe,
    subscribeMutation,
    onCancel,
    cancelMutation,
  } = usePlan({ planId: plan.id, period: period });

  return (
    <>
      <PlanInfo plan={plan} />
      <Box>
        {activePlan ? (
          <ActivePlanInfo plan={activePlan} />
        ) : (
          t({
            key: 'billing_plan_price',
            defaultValue:
              'Price: {price, number, :: currency/EUR} per {period, select, MONTHLY {month} other {year}}',
            parameters: {
              period: period,
              price:
                (period === 'MONTHLY' ? plan.monthlyPrice : plan.yearlyPrice) ||
                0,
            },
          })
        )}
      </Box>

      {!plan.free &&
        (activePlan ? (
          <LoadingButton
            loading={cancelMutation.isLoading}
            variant="outlined"
            color="primary"
            onClick={() => onCancel()}
          >
            Cancel subscription
          </LoadingButton>
        ) : isOrganizationSubscribed ? (
          <LoadingButton
            loading={upgradeMutation.isLoading}
            variant="outlined"
            color="primary"
            onClick={() => onUpgrade()}
          >
            Subscribe
          </LoadingButton>
        ) : (
          <LoadingButton
            loading={subscribeMutation.isLoading}
            variant="outlined"
            color="primary"
            onClick={() => onSubscribe()}
          >
            Subscribe
          </LoadingButton>
        ))}
    </>
  );
};
