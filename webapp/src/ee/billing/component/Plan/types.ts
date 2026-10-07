import { components } from 'tg.service/billingApiSchema.generated';

type CloudPlanTierModel = components['schemas']['CloudPlanTierModel'];

// Also the shape of the self-hosted plan models and of the synthetic Enterprise card, neither of
// which carries prices, included usage or an onboarding boost.
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
