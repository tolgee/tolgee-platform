import { useBillingApiMutation } from 'tg.service/http/useQueryApi';
import { useOrganization } from 'tg.views/organizations/useOrganization';

export const useGoToStripeCustomerPortal = () => {
  const organization = useOrganization();

  const getCustomerPortalSession = useBillingApiMutation({
    url: '/v2/organizations/{organizationId}/billing/customer-portal',
    method: 'get',
    options: {
      onSuccess: (data) => {
        window.location.href = data.url;
      },
    },
  });

  return () =>
    getCustomerPortalSession.mutate({
      path: {
        organizationId: organization!.id,
      },
    });
};
