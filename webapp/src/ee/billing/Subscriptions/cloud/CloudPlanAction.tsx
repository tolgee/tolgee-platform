import { Box, styled, Tooltip } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { PrepareUpgradeDialog } from '../../PrepareUpgradeDialog';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { BillingPeriodType } from '../../component/Price/PeriodSwitch';
import { usePlan } from '../../component/Plan/usePlan';
import { useCancelCloudSubscription } from './useCancelCloudSubscription';
import { useRestoreCloudSubscription } from './useRestoreCloudSubscription';

export const CloudPlanActionContainer = styled(Box)`
  justify-self: center;
  align-self: end;
  gap: 8px;
  display: flex;
  justify-content: flex-end;
  flex-wrap: wrap;
  white-space: nowrap;
`;

type Props = {
  activeTrial: boolean;
  hasActivePaidSubscription: boolean;
  active: boolean;
  cancelAtPeriodEnd: boolean;
  custom?: boolean;
  planId: number;
  period: BillingPeriodType;
  show?: boolean;
};

export const PlanAction = ({
  active,
  cancelAtPeriodEnd,
  custom,
  hasActivePaidSubscription,
  planId,
  period,
  show,
  activeTrial,
}: Props) => {
  const {
    prepareUpgradeMutation,
    subscribeMutation,
    onPrepareUpgrade,
    onSubscribe,
  } = usePlan({
    planId: planId,
    period: period,
  });

  const { t } = useTranslate();

  const { cancelMutation, doCancel } = useCancelCloudSubscription();
  const { restoreMutation, onRestore } = useRestoreCloudSubscription();

  const getLabelAndAction = () => {
    if (active && !cancelAtPeriodEnd && !activeTrial) {
      return {
        loading: cancelMutation.isLoading,
        onClick: doCancel,
        label: t('billing_plan_cancel'),
      };
    } else if (active && cancelAtPeriodEnd && !activeTrial) {
      return {
        loading: restoreMutation.isLoading,
        onClick: onRestore,
        label: t('billing_plan_resubscribe'),
      };
    } else if (hasActivePaidSubscription) {
      return {
        loading: prepareUpgradeMutation.isLoading,
        onClick: () => onPrepareUpgrade(),
        label: t('billing_plan_subscribe'),
      };
    } else {
      return {
        loading: subscribeMutation.isLoading,
        onClick: () => onSubscribe(),
        label: t('billing_plan_subscribe'),
        tooltip: activeTrial && (
          <span data-cy="subscribe-cancels-trial-plan-tooltip">
            {t('billing_plan_subscribe_trial_tooltip')}
          </span>
        ),
      };
    }
  };

  const { loading, onClick, label, tooltip } = getLabelAndAction();
  const shouldShow = show == undefined || show;

  return (
    <CloudPlanActionContainer>
      {shouldShow && (
        <Tooltip title={tooltip}>
          <span>
            <LoadingButton
              data-cy="billing-plan-action-button"
              variant="contained"
              color={custom ? 'info' : 'primary'}
              size="medium"
              loading={loading}
              onClick={onClick}
            >
              {label}
            </LoadingButton>
          </span>
        </Tooltip>
      )}
      {prepareUpgradeMutation.data && (
        <PrepareUpgradeDialog
          data={prepareUpgradeMutation.data}
          onClose={() => {
            prepareUpgradeMutation.reset();
          }}
        />
      )}
    </CloudPlanActionContainer>
  );
};
