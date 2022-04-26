import { FunctionComponent } from 'react';
import { T } from '@tolgee/react';

import { BaseOrganizationSettingsView } from 'tg.views/organizations/BaseOrganizationSettingsView';

import { useOrganization } from 'tg.views/organizations/useOrganization';
import { useLocation } from 'react-router-dom';
import { MessageService } from 'tg.service/MessageService';
import { container } from 'tsyringe';
import { Button } from '@mui/material';
import {
  useBillingApiMutation,
  useBillingApiQuery,
} from './useBillingQueryApi';

const messaging = container.resolve(MessageService);
export const OrganizationBillingView: FunctionComponent = () => {
  const { search } = useLocation();
  const params = new URLSearchParams(search);

  const success = params.has('success');
  const canceled = params.has('canceled');

  const url = new URL(window.location.href);
  url.search = '';

  const getSubscribeSession = useBillingApiMutation({
    url: '/v2/billing/create-checkout-session',
    method: 'post',
    options: {
      onSuccess: (data) => {
        window.location.href = data;
      },
      onError: (data) => {
        if (data.code === 'organization_already_subscribed') {
          messaging.error(
            <T keyName="billing_organization_already_subscribed" />
          );
        }
      },
    },
  });

  const getCustomerPortalSession = useBillingApiMutation({
    url: '/v2/billing/create-customer-portal-session',
    method: 'post',
    options: {
      onSuccess: (data) => {
        window.location.href = data;
      },
    },
  });

  const organization = useOrganization();

  const stateLoadable = useBillingApiQuery({
    url: '/v2/billing/subscription-state/{organizationId}',
    method: 'get',
    path: {
      organizationId: organization!.id,
    },
  });

  const onSubscribe = () => {
    getSubscribeSession.mutate({
      content: {
        'application/json': {
          cancelUrl: `${url}?canceled`,
          successUrl: `${url}?success`,
          organizationId: organization!.id,
        },
      },
    });
  };

  return (
    <BaseOrganizationSettingsView
      title={<T>organization_billing_title</T>}
      hideChildrenOnLoading={true}
      loading={stateLoadable.isLoading}
    >
      <Button variant={'outlined'} onClick={onSubscribe}>
        {/*todo: translate */}
        Subscribe...
      </Button>

      <Button
        variant={'outlined'}
        onClick={() =>
          getCustomerPortalSession.mutate({
            content: {
              'application/json': {
                organizationId: organization!.id,
                returnUrl: url.href,
              },
            },
          })
        }
      >
        {/*todo: translate */}
        Go to customer portal
      </Button>

      {JSON.stringify(stateLoadable?.data)}
    </BaseOrganizationSettingsView>
  );
};
