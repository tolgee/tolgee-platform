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
  const queryBase = {
    url: '/v2/administration/billing/self-hosted-ee-plans/{planId}/subscriptions',
    method: 'get',
    path: { planId: plan.id },
    query: { page: 0, size: 30 },
  } as const;

  const subscriptions = useBillingApiInfiniteQuery<any, any>({
    ...queryBase,
    options: {
      enabled,
      keepPreviousData: true,
      getNextPageParam: (lastPage: any) => {
        if (
          lastPage.page &&
          lastPage.page.number! < lastPage.page.totalPages! - 1
        ) {
          return {
            ...queryBase,
            query: { ...queryBase.query, page: lastPage.page.number! + 1 },
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
