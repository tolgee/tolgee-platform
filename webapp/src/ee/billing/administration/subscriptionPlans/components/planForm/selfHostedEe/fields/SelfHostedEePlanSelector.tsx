import React, { FC } from 'react';
import { useBillingApiQuery } from 'tg.service/http/useQueryApi';
import { components } from 'tg.service/billingApiSchema.generated';
import { GenericPlanSelector } from '../../genericFields/GenericPlanSelector';

export const SelfHostedEePlanSelector: FC<
  Omit<
    GenericPlanSelector<
      components['schemas']['SelfHostedEePlanAdministrationModel']
    >,
    'plans' | 'loading'
  > & { organizationId?: number }
> = ({ organizationId, ...props }) => {
  const plansLoadable = useBillingApiQuery({
    url: '/v2/administration/billing/self-hosted-ee-plans',
    method: 'get',
    query: {
      filterAssignableToOrganization: organizationId,
    },
  });

  return (
    <GenericPlanSelector
      plans={plansLoadable.data?._embedded?.plans}
      loading={plansLoadable.isLoading}
      {...props}
    />
  );
};
