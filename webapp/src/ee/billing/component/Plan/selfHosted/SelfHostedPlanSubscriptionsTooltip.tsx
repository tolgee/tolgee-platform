import { useState } from 'react';
import { PlanSubscriptionsTooltip } from 'tg.ee.module/billing/component/Plan/PlanSubscriptionsTooltip';
import { useBillingApiInfiniteQuery } from 'tg.service/http/useQueryApi';
import { components } from 'tg.service/billingApiSchema.generated';

type SelfHostedPlanModel =
  components['schemas']['SelfHostedEePlanAdministrationModel'];

export const SelfHostedPlanSubscriptionsTooltip = ({
  plan,
}: {
  plan: SelfHostedPlanModel;
}) => {
  const [enabled, setEnabled] = useState(false);
  const query = { page: 0, size: 30 };
  const subscriptions = useBillingApiInfiniteQuery({
    url: '/v2/administration/billing/self-hosted-ee-plans/{planId}/subscriptions',
    method: 'get',
    path: { planId: plan.id },
    query,
    options: {
      enabled,
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
