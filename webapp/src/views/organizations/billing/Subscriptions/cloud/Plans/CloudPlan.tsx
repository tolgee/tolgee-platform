import { FC } from 'react';
import { Box } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import clsx from 'clsx';

import { components } from 'tg.service/billingApiSchema.generated';
import { CloudPlanInfo } from './CloudPlanInfo';
import { usePlan } from './usePlan';
import { PlanActionButton } from './PlanActionButton';
import { PlanTitle } from '../../common/PlanTitle';
import { PeriodPlanPrice } from './PeriodPlanPrice';
import { PrepareUpgradeDialog } from '../../../PrepareUpgradeDialog';
import { BillingPeriodType, PeriodSwitch } from './PeriodSwitch';
import { Plan, PlanContent, PlanSubtitle } from '../../common/Plan';
import { confirmation } from 'tg.hooks/confirmation';
import { PlanPrice } from './PlanPrice';

type PlanModel = components['schemas']['CloudPlanModel'];

type Props = {
  plan: PlanModel;
  isOrganizationSubscribed: boolean;
  period: BillingPeriodType;
  onPeriodChange: (period: BillingPeriodType) => void;
  isActive: boolean;
  isEnded: boolean;
};

export const CloudPlan: FC<Props> = ({
  plan,
  isOrganizationSubscribed,
  period,
  onPeriodChange,
  isActive,
  isEnded,
}) => {
  const { t } = useTranslate();
  const {
    onPrepareUpgrade,
    prepareUpgradeMutation,
    onSubscribe,
    subscribeMutation,
    onCancel,
    cancelMutation,
  } = usePlan({ planId: plan.id, period: period });

  const handleCancel = () => {
    confirmation({
      title: <T keyName="billing_cancel_dialog_title" />,
      message: <T keyName="billing_cancel_dialog_message" />,
      onConfirm: onCancel,
    });
  };

  return (
    <Plan className={clsx({ active: isActive })} data-cy="billing-plan">
      {isActive && (
        <PlanSubtitle data-cy="billing-plan-subtitle">
          {isEnded
            ? t('billing_subscription_cancelled')
            : t('billing_subscription_active')}
        </PlanSubtitle>
      )}
      <PlanContent>
        <PlanTitle title={plan.name} />

        <CloudPlanInfo plan={plan} />

        <Box minHeight="19px" gridArea="period-switch">
          {Boolean(
            plan.prices.subscriptionMonthly || plan.prices.subscriptionMonthly
          ) && <PeriodSwitch value={period} onChange={onPeriodChange} />}
        </Box>
        <PlanPrice prices={plan.prices} period={period} />

        {!plan.free &&
          (isActive && !isEnded ? (
            <PlanActionButton
              loading={cancelMutation.isLoading}
              onClick={handleCancel}
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
        {prepareUpgradeMutation.data && (
          <PrepareUpgradeDialog
            data={prepareUpgradeMutation.data}
            onClose={() => {
              prepareUpgradeMutation.reset();
            }}
          />
        )}
      </PlanContent>
    </Plan>
  );
};
