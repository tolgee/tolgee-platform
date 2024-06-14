import { Box, styled } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { PrepareUpgradeDialog } from 'tg.views/organizations/billing/PrepareUpgradeDialog';

import type { usePlan } from './usePlan';
import { confirmation } from 'tg.hooks/confirmation';
import LoadingButton from 'tg.component/common/form/LoadingButton';

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
  actions: ReturnType<typeof usePlan>;
  organizationHasSomeSubscription: boolean;
  isActive: boolean;
  isEnded: boolean;
};

export const PlanAction = ({
  isActive,
  isEnded,
  actions: {
    cancelMutation,
    prepareUpgradeMutation,
    subscribeMutation,
    onPrepareUpgrade,
    onSubscribe,
    onCancel,
  },
  organizationHasSomeSubscription,
}: Props) => {
  const { t } = useTranslate();

  const handleCancel = () => {
    confirmation({
      title: <T keyName="billing_cancel_dialog_title" />,
      message: <T keyName="billing_cancel_dialog_message" />,
      onConfirm: onCancel,
    });
  };

  function getLabelAndAction() {
    if (isActive && !isEnded) {
      return {
        loading: cancelMutation.isLoading,
        onClick: handleCancel,
        label: t('billing_plan_cancel'),
      };
    } else if (isActive && isEnded) {
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

  return (
    <StyledContainer>
      <LoadingButton
        data-cy="billing-plan-action-button"
        variant="contained"
        color="primary"
        size="small"
        loading={loading}
        onClick={onClick}
      >
        {label}
      </LoadingButton>
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
