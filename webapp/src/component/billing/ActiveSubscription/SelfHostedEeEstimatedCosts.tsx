import { FC } from 'react';
import { components } from 'tg.service/billingApiSchema.generated';
import { useBillingApiQuery } from 'tg.service/http/useQueryApi';
import { PlanUsageEstimatedCosts } from 'tg.ee/billing/common/usage/PlanUsageEstimatedCosts';
import { useOrganization } from 'tg.views/organizations/useOrganization';

export const SelfHostedEeEstimatedCosts: FC<{
  subscription: components['schemas']['SelfHostedEeSubscriptionModel'];
}> = ({ subscription }) => {
  const organization = useOrganization();

  const useUsage = (enabled: boolean) =>
    useBillingApiQuery({
      url: '/v2/organizations/{organizationId}/billing/self-hosted-ee/subscriptions/{subscriptionId}/expected-usage',
      method: 'get',
      path: {
        organizationId: organization!.id,
        subscriptionId: subscription.id,
      },
      options: {
        enabled,
      },
    });

  return (
    <PlanUsageEstimatedCosts
      useUsage={useUsage}
      estimatedCosts={subscription.estimatedCosts}
    />
  );
};
