import { FunctionComponent } from 'react';
import { T } from '@tolgee/react';

import { BaseOrganizationSettingsView } from './BaseOrganizationSettingsView';
import Button from '@material-ui/core/Button';
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import { useOrganization } from './useOrganization';
import { components } from 'tg.service/apiSchema.generated';
import { useLocation } from 'react-router-dom';
import { MessageService } from 'tg.service/MessageService';
import { container } from 'tsyringe';

const messaging = container.resolve(MessageService);
export const OrganizationBillingView: FunctionComponent = () => {
  const { search } = useLocation();
  const params = new URLSearchParams(search);

  const success = params.has('success');
  const canceled = params.has('canceled');

  const url = new URL(window.location.href);
  url.search = '';

  const getSubscribeSession = useApiMutation({
    url: '/v2/billing/create-checkout-session',
    method: 'post',
    options: {
      onSuccess: (data) => (window.location = data),
      onError: (data) => {
        if (data.code === 'organization_already_subscribed') {
          messaging.error(
            <T keyName="billing_organization_already_subscribed" />
          );
        }
      },
    },
  });

  const getCustomerPortalSession = useApiMutation({
    url: '/v2/billing/create-customer-portal-session',
    method: 'post',
    options: {
      onSuccess: (data) => (window.location = data),
    },
  });

  const organization = useOrganization();

  const stateLoadable = useApiQuery({
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
        } as components['schemas']['CreateCheckoutSessionRequest'],
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
                returnUrl: url,
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
