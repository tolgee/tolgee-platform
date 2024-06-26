import { T, useTranslate } from '@tolgee/react';
import { LoadingButton } from '@mui/lab';
import { Box } from '@mui/material';

import { useBillingApiMutation } from 'tg.service/http/useQueryApi';
import { useSuccessMessage } from 'tg.hooks/useSuccessMessage';
import { confirmation } from 'tg.hooks/confirmation';
import { useOrganization } from 'tg.views/organizations/useOrganization';
import { PlanLicenseKey } from 'tg.component/billing/ActiveSubscription/PlanLicenseKey';

type Props = {
  id: number;
  licenceKey: string | undefined;
  isNew: boolean;
  custom: boolean;
};

export const SelfHostedEeSubscriptionActions = ({
  id,
  licenceKey,
  isNew,
  custom,
}: Props) => {
  const { t } = useTranslate();

  const successMessage = useSuccessMessage();

  const organization = useOrganization();

  const cancelMutation = useBillingApiMutation({
    url: '/v2/organizations/{organizationId}/billing/self-hosted-ee/subscriptions/{subscriptionId}',
    method: 'delete',
    invalidatePrefix: '/v2/organizations/{organizationId}/billing',
    options: {
      onSuccess: () => {
        successMessage(
          <T keyName="organization-billing-self-hosted-subscription-cancelled-message" />
        );
      },
    },
  });

  function onClick() {
    confirmation({
      message: (
        <T keyName="organization-billing-self-hosted-subscription-cancel-confirmation" />
      ),
      onConfirm: () => {
        cancelMutation.mutate({
          path: { subscriptionId: id, organizationId: organization!.id },
        });
      },
    });
  }

  return (
    <Box display="flex" gap={1}>
      <LoadingButton
        onClick={onClick}
        variant="outlined"
        loading={cancelMutation.isLoading}
        size="small"
        color={custom ? 'info' : 'primary'}
      >
        {t('organization-billing-self-hosted-cancel-subscription-button')}
      </LoadingButton>
      <PlanLicenseKey
        licenseKey={licenceKey}
        defaultOpen={isNew}
        custom={custom}
      />
    </Box>
  );
};
