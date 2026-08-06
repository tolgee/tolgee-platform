import { components } from 'tg.service/billingApiSchema.generated';

type CloudPlanTierModel = components['schemas']['CloudPlanTierModel'];

export type PlanType = Omit<
  CloudPlanTierModel,
  'prices' | 'includedUsage' | 'type'
> & {
  prices?: CloudPlanTierModel['prices'];
  includedUsage?: CloudPlanTierModel['includedUsage'];
  type?: CloudPlanTierModel['type'] | 'CONTACT_US';
};
