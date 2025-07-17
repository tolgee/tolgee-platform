import React, { FC } from 'react';
import { useBillingApiQuery } from 'tg.service/http/useQueryApi';
import { components } from 'tg.service/billingApiSchema.generated';
import { GenericPlanSelector } from '../../genericFields/GenericPlanSelector';
import { SearchSelect } from 'tg.component/searchSelect/SearchSelect';

export const CloudPlanSelector: FC<
  Omit<
    GenericPlanSelector<components['schemas']['AdministrationCloudPlanModel']>,
    'plans'
  > & {
    organizationId?: number;
    selectProps?: React.ComponentProps<typeof SearchSelect>[`SelectProps`];
    filterPublic?: boolean;
    filterHasMigration?: boolean;
  }
> = ({ organizationId, filterPublic, filterHasMigration, ...props }) => {
  const plansLoadable = useBillingApiQuery({
    url: '/v2/administration/billing/cloud-plans',
    method: 'get',
    query: {
      filterPublic,
      filterHasMigration,
    },
  });

  return (
    <GenericPlanSelector
      plans={plansLoadable?.data?._embedded?.plans}
      {...props}
    />
  );
};
