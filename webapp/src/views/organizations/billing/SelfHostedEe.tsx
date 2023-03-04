import { useTranslate } from '@tolgee/react';
import { Button, Dialog } from '@mui/material';
import {
  useBillingApiMutation,
  useBillingApiQuery,
} from 'tg.service/http/useQueryApi';
import { useOrganization } from '../useOrganization';
import { useState } from 'react';

export const SelfHostedEe = () => {
  const { t } = useTranslate();

  const organization = useOrganization();

  const plansLoadable = useBillingApiQuery({
    url: `/v2/organizations/{organizationId}/billing/self-hosted-ee-plans`,
    method: 'get',
    path: {
      organizationId: organization!.id,
    },
  });

  const setupMutation = useBillingApiMutation({
    url: '/v2/organizations/{organizationId}/billing/setup-ee',
    method: 'post',
    options: {
      onSuccess: (data) => {
        window.location.href = data.url;
      },
    },
  });

  const [open, setOpen] = useState(false);

  return (
    <>
      <Button
        onClick={() => {
          setOpen(true);
        }}
      >
        {t('organization-billing-get-self-hosted-key')}
      </Button>

      <Dialog open={open} onClose={() => setOpen(false)}>
        {plansLoadable.data?._embedded?.plans?.map((plan) => (
          <Button
            key={plan.id}
            onClick={() => {
              setupMutation.mutate({
                path: { organizationId: organization!.id },
                content: {
                  'application/json': { planId: plan.id },
                },
              });
            }}
          >
            Setup plan {plan.name}
          </Button>
        ))}
      </Dialog>
    </>
  );
};
