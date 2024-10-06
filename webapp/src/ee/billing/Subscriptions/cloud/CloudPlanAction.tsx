import { Box, styled } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { PrepareUpgradeDialog } from '../../PrepareUpgradeDialog';

import { usePlan } from 'tg.component/billing/Plan/usePlan';
import { confirmation } from 'tg.hooks/confirmation';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { BillingPeriodType } from 'tg.component/billing/Price/PeriodSwitch';

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
  organizationHasSomeSubscription: boolean;
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
  organizationHasSomeSubscription,
  planId,
  period,
  show,
}: Props) => {
  const {
    cancelMutation,
    prepareUpgradeMutation,
    subscribeMutation,
    onCancel,
    onPrepareUpgrade,
    onSubscribe,
  } = usePlan({
    planId: planId,
    period: period,
  });

  const { t } = useTranslate();

  const handleCancel = () => {
    confirmation({
      title: <T keyName="billing_cancel_dialog_title" />,
      message: <T keyName="billing_cancel_dialog_message" />,
      onConfirm: onCancel,
    });
  };

  function getLabelAndAction() {
    if (active && !ended) {
      return {
        loading: cancelMutation.isLoading,
        onClick: handleCancel,
        label: t('billing_plan_cancel'),
      };
    } else if (active && ended) {
      return {
        loading: prepareUpgradeMutation.isLoading,
        onClick: () => onPrepareUpgrade(),
        label: t('billing_plan_resubscribe'),
      };
    } else if (organizationHasSomeSubscription) {
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
