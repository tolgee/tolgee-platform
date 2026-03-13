import { useBillingApiInfiniteQuery } from 'tg.service/http/useQueryApi';
import { PlanSubscriptionsTooltip } from 'tg.ee.module/billing/component/Plan/PlanSubscriptionsTooltip';
import { components } from 'tg.service/billingApiSchema.generated';
import { useState } from 'react';

type CloudPlanModel = components['schemas']['AdministrationCloudPlanModel'];

export const CloudPlanSubscriptionsTooltip = ({
  plan,
}: {
  plan: CloudPlanModel;
}) => {
  const [enabled, setEnabled] = useState(false);
  const query = { page: 0, size: 10 };
  const subscriptions = useBillingApiInfiniteQuery({
    url: '/v2/administration/billing/cloud-plans/{planId}/subscriptions',
    method: 'get',
    path: { planId: plan.id },
    query,
    options: {
      enabled: enabled,
      keepPreviousData: true,
      getNextPageParam: (lastPage) => {
        if (
          lastPage.page &&
          lastPage.page.number! < lastPage.page.totalPages! - 1
        ) {
          return {
            query: { ...query, page: lastPage.page.number! + 1 },
            path: { planId: plan.id },
          };
        }
        return null;
      },
    },
  });

  return (
    <PlanSubscriptionsTooltip
      plan={plan}
      subscriptions={subscriptions}
      onOpen={() => setEnabled(true)}
    />
  );
};
