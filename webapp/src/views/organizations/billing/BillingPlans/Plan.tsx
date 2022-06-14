import { FC } from 'react';
import { styled } from '@mui/material';
import { useTranslate } from '@tolgee/react';

import { components } from 'tg.service/billingApiSchema.generated';
import { PlanInfo } from './PlanInfo';
import { usePlan } from './usePlan';
import { PlanActionButton } from './PlanActionButton';
import { PlanTitle } from './PlanTitle';
import { PlanPrice } from './PlanPrice';
import { PrepareUpgradeDialog } from '../PrepareUpgradeDialog';

type PlanModel = components['schemas']['PlanModel'];
type Period = components['schemas']['SubscribeRequest']['period'];

const StyledPlan = styled('div')`
  background: ${({ theme }) => theme.palette.emphasis[200]};
  border: 1px solid ${({ theme }) => theme.palette.emphasis[200]};
  border-radius: 20px;
  padding: 20px;
  display: grid;
  gap: 8px;
  grid-template-rows: 1fr auto auto;
  grid-template-areas:
    'title    title'
    'info     info'
    'price    action';
`;

type Props = {
  plan: PlanModel;
  isOrganizationSubscribed: boolean;
  period: Period;
  isActive: boolean;
  isEnded: boolean;
};

export const Plan: FC<Props> = ({
  plan,
  isOrganizationSubscribed,
  period,
  isActive,
  isEnded,
}) => {
  const t = useTranslate();
  const {
    onPrepareUpgrade,
    prepareUpgradeMutation,
    onSubscribe,
    subscribeMutation,
    onCancel,
    cancelMutation,
  } = usePlan({ planId: plan.id, period: period });

  return (
    <StyledPlan>
      <PlanTitle
        title={plan.name}
        subtitle={
          isActive
            ? isEnded
              ? t('billing_subscription_cancelled')
              : t('billing_subscription_active')
            : undefined
        }
      />

      <PlanInfo plan={plan} />
      <PlanPrice
        price={period === 'MONTHLY' ? plan.monthlyPrice : plan.yearlyPrice}
        period={period}
      />

      {prepareUpgradeMutation.data && (
        <PrepareUpgradeDialog
          data={prepareUpgradeMutation.data}
          onClose={() => {
            prepareUpgradeMutation.reset();
          }}
        />
      )}

      {!plan.free &&
        (isActive && !isEnded ? (
          <PlanActionButton
            loading={cancelMutation.isLoading}
            onClick={() => onCancel()}
          >
            {t('billing_plan_cancel')}
          </PlanActionButton>
        ) : isActive && isEnded ? (
          <PlanActionButton
            loading={prepareUpgradeMutation.isLoading}
            onClick={() => onPrepareUpgrade()}
          >
            {t('billing_plan_resubscribe')}
          </PlanActionButton>
        ) : isOrganizationSubscribed ? (
          <PlanActionButton
            loading={prepareUpgradeMutation.isLoading}
            onClick={() => onPrepareUpgrade()}
          >
            {t('billing_plan_subscribe')}
          </PlanActionButton>
        ) : (
          <PlanActionButton
            loading={subscribeMutation.isLoading}
            onClick={() => onSubscribe()}
          >
            {t('billing_plan_subscribe')}
          </PlanActionButton>
        ))}
    </StyledPlan>
  );
};
