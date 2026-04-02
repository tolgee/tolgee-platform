import { components } from 'tg.service/billingApiSchema.generated';

type CloudPlanModel = components['schemas']['CloudPlanModel'];

export type PlanType = Omit<
  CloudPlanModel,
  'prices' | 'includedUsage' | 'type'
> & {
  prices?: CloudPlanModel['prices'];
  includedUsage?: CloudPlanModel['includedUsage'];
  type?: CloudPlanModel['type'] | 'CONTACT_US';
};
