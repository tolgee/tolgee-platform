import React, { FC } from 'react';
import { useBillingApiQuery } from 'tg.service/http/useQueryApi';
import { components } from 'tg.service/billingApiSchema.generated';
import { GenericPlanSelector } from '../../genericFields/GenericPlanSelector';
import { SearchSelect } from 'tg.component/searchSelect/SearchSelect';

export const CloudPlanSelector: FC<
  Omit<
    GenericPlanSelector<components['schemas']['AdministrationCloudPlanModel']>,
    'plans' | 'loading'
  > & {
    organizationId?: number;
    selectProps?: React.ComponentProps<typeof SearchSelect>[`SelectProps`];
    filterPublic?: boolean;
  }
> = ({ organizationId, filterPublic, ...props }) => {
  const plansLoadable = useBillingApiQuery({
    url: '/v2/administration/billing/cloud-plans',
    method: 'get',
    query: {
      filterPublic,
    },
  });

  return (
    <GenericPlanSelector
      plans={plansLoadable?.data?._embedded?.plans}
      {...props}
      loading={plansLoadable.isLoading}
    />
  );
};
