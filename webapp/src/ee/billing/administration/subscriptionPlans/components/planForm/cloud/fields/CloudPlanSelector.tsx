import React, { FC } from 'react';
import { useBillingApiQuery } from 'tg.service/http/useQueryApi';
import { components } from 'tg.service/billingApiSchema.generated';
import { GenericPlanSelector } from '../../genericFields/GenericPlanSelector';

export const CloudPlanSelector: FC<
  Omit<
    GenericPlanSelector<components['schemas']['AdministrationCloudPlanModel']>,
    'plans'
  > & { organizationId?: number }
> = ({ organizationId, ...props }) => {
  const plansLoadable = useBillingApiQuery({
    url: '/v2/administration/billing/cloud-plans',
    method: 'get',
    query: {
      filterAssignableToOrganization: organizationId,
    },
  });

  if (!plansLoadable?.data?._embedded?.plans) {
    return null;
  }

  return (
    <GenericPlanSelector
      plans={plansLoadable.data._embedded.plans}
      {...props}
    />
  );
};
