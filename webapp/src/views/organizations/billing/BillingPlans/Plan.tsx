import { FC } from 'react';
import { Box } from '@mui/material';
import { useTranslate, T } from '@tolgee/react';
import clsx from 'clsx';

import { components } from 'tg.service/billingApiSchema.generated';
import { PlanInfo } from './PlanInfo';
import { usePlan } from './usePlan';
import { PlanActionButton } from './PlanActionButton';
import { PlanTitle } from './PlanTitle';
import { PlanPrice } from './PlanPrice';
import { PrepareUpgradeDialog } from '../PrepareUpgradeDialog';
import { PeriodSwitch, BillingPeriodType } from './PeriodSwitch';
import { StyledPlan, StyledSubtitle, StyledContent } from './StyledPlan';
import { confirmation } from 'tg.hooks/confirmation';

type PlanModel = components['schemas']['PlanModel'];
type Period = components['schemas']['SubscribeRequest']['period'];

type Props = {
  plan: PlanModel;
  isOrganizationSubscribed: boolean;
  period: Period;
  onPeriodChange: (period: BillingPeriodType) => void;
  isActive: boolean;
  isEnded: boolean;
};

export const Plan: FC<Props> = ({
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
    <StyledPlan className={clsx({ active: isActive })} data-cy="billing-plan">
      {isActive && (
        <StyledSubtitle data-cy="billing-plan-subtitle">
          {isEnded
            ? t('billing_subscription_cancelled')
            : t('billing_subscription_active')}
        </StyledSubtitle>
      )}
      <StyledContent>
        <PlanTitle title={plan.name} />

        <PlanInfo plan={plan} />

        <Box gridArea="action" display="grid">
          <Box minHeight="19px">
            {!plan.free && (
              <PeriodSwitch value={period} onChange={onPeriodChange} />
            )}
          </Box>
          <Box
            display="flex"
            justifyContent="space-between"
            alignItems="center"
          >
            <PlanPrice
              price={
                period === 'MONTHLY' ? plan.monthlyPrice : plan.yearlyPrice / 12
              }
              period={period}
            />

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
          </Box>
        </Box>
        {prepareUpgradeMutation.data && (
          <PrepareUpgradeDialog
            data={prepareUpgradeMutation.data}
            onClose={() => {
              prepareUpgradeMutation.reset();
            }}
          />
        )}
      </StyledContent>
    </StyledPlan>
  );
};
