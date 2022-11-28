import { T, useTranslate } from '@tolgee/react';

import { useBillingApiMutation } from 'tg.service/http/useQueryApi';
import { useSuccessMessage } from 'tg.hooks/useSuccessMessage';
import { confirmation } from 'tg.hooks/confirmation';
import { useOrganization } from '../useOrganization';
import { StyledActionArea } from './BillingSection';
import { LoadingButton } from '@mui/lab';
import { PlanLicenseKey } from './Subscriptions/selfHostedEe/PlanLicenseKey';

type Props = {
  id: number;
  licenceKey: string | undefined;
};

export const SelfHostedEeSubscriptionActions = ({ id, licenceKey }: Props) => {
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
    <StyledActionArea>
      <PlanLicenseKey licenseKey={licenceKey} />
      <LoadingButton
        onClick={onClick}
        variant="outlined"
        loading={cancelMutation.isLoading}
        size="small"
        color="primary"
      >
        {t('organization-billing-self-hosted-cancel-subscription-button')}
      </LoadingButton>
    </StyledActionArea>
  );
};
