import { Box, styled } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { PrepareUpgradeDialog } from '../../PrepareUpgradeDialog';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { BillingPeriodType } from '../../component/Price/PeriodSwitch';
import { usePlan } from '../../component/Plan/usePlan';
import { useCancelCloudSubscription } from './useCancelCloudSubscription';
import { useRestoreCloudSubscription } from './useRestoreCloudSubscription';

export const StyledContainer = styled(Box)`
  justify-self: center;
  align-self: end;
  gap: 8px;
  display: flex;
  justify-content: flex-end;
  flex-wrap: wrap;
  white-space: nowrap;
`;

type Props = {
  hasActivePaidSubscription: boolean;
  active: boolean;
  ended: boolean;
  custom?: boolean;
  planId: number;
  period: BillingPeriodType;
  show?: boolean;
};

export const PlanAction = ({
  active,
  ended,
  custom,
  hasActivePaidSubscription,
  planId,
  period,
  show,
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

  function getLabelAndAction() {
    if (active && !ended) {
      return {
        loading: cancelMutation.isLoading,
        onClick: doCancel,
        label: t('billing_plan_cancel'),
      };
    } else if (active && ended) {
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
      };
    }
  }

  const { loading, onClick, label } = getLabelAndAction();
  const shouldShow = show == undefined || show;

  return (
    <StyledContainer>
      {shouldShow && (
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
      )}
      {prepareUpgradeMutation.data && (
        <PrepareUpgradeDialog
          data={prepareUpgradeMutation.data}
          onClose={() => {
            prepareUpgradeMutation.reset();
          }}
        />
      )}
    </StyledContainer>
  );
};
