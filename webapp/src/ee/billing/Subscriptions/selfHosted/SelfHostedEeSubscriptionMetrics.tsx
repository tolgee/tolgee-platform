import React, { FC } from 'react';
import { SubscriptionMetrics } from '../../currentCloudSubscription/SubscriptionMetrics';
import { components } from 'tg.service/billingApiSchema.generated';
import { getSelfHostedProgressData } from '../../getSelfHostedProgressData';
import { useOrganization } from 'tg.views/organizations/useOrganization';
import { useBillingApiQuery } from 'tg.service/http/useQueryApi';
import { BoxLoading } from 'tg.component/common/BoxLoading';
import { styled } from '@mui/material';

type SelfHostedSubscriptionMetricsProps = {
  subscription: components['schemas']['SelfHostedEeSubscriptionModel'];
};

const StyledContent = styled('div')`
  display: grid;
  grid-template-columns: auto auto 1fr;
  gap: 6px 16px;
  flex-wrap: wrap;
  margin: 4px 0px 32px 0px;
`;

export const SelfHostedEeSubscriptionMetrics: FC<
  SelfHostedSubscriptionMetricsProps
> = ({ subscription }) => {
  const organization = useOrganization();
  const usageLoadable = useBillingApiQuery({
    url: '/v2/organizations/{organizationId}/billing/self-hosted-ee/subscriptions/{subscriptionId}/current-usage',
    method: 'get',
    path: {
      organizationId: organization!.id,
      subscriptionId: subscription.id,
    },
  });

  if (usageLoadable.isLoading) {
    return <BoxLoading />;
  }

  if (!usageLoadable.data) {
    return null;
  }

  const progressData = getSelfHostedProgressData({ usage: usageLoadable.data });

  return (
    <StyledContent data-cy="self-hosted-ee-subscription-metrics">
      <SubscriptionMetrics
        metricType="KEYS_SEATS"
        progressData={progressData}
        isPayAsYouGo={subscription.plan.isPayAsYouGo}
      />
    </StyledContent>
  );
};
