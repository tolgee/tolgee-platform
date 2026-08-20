import { components } from 'tg.service/billingApiSchema.generated';

type CloudPlanTierModel = components['schemas']['CloudPlanTierModel'];

// The self-hosted plan models and the synthetic Enterprise card share this shape, and neither
// carries an onboarding boost — it is a cloud-only offering field.
export type PlanType = Omit<
  CloudPlanTierModel,
  | 'prices'
  | 'includedUsage'
  | 'type'
  | 'onboardingBoostMonths'
  | 'onboardingBoostCredits'
> & {
  prices?: CloudPlanTierModel['prices'];
  includedUsage?: CloudPlanTierModel['includedUsage'];
  type?: CloudPlanTierModel['type'] | 'CONTACT_US';
  onboardingBoostMonths?: CloudPlanTierModel['onboardingBoostMonths'];
  onboardingBoostCredits?: CloudPlanTierModel['onboardingBoostCredits'];
};
