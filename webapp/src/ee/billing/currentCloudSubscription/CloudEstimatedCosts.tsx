import { FC } from 'react';
import { useOrganization } from 'tg.views/organizations/useOrganization';
import { useBillingApiQuery } from 'tg.service/http/useQueryApi';
import { EstimatedCosts } from '../common/usage/EstimatedCosts';

export const CloudEstimatedCosts: FC<{ estimatedCosts: number }> = (props) => {
  const organization = useOrganization();

  const useUsage = (enabled: boolean) =>
    useBillingApiQuery({
      url: '/v2/organizations/{organizationId}/billing/expected-usage',
      method: 'get',
      path: {
        organizationId: organization!.id,
      },
      options: {
        enabled,
      },
    });

  return <EstimatedCosts {...props} useUsage={useUsage} />;
};
