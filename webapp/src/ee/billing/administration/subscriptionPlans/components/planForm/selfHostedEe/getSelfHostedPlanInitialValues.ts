import { SelfHostedEePlanFormData } from '../cloud/types';

export function getSelfHostedPlanInitialValues(): SelfHostedEePlanFormData {
  return {
    name: '',
    stripeProductId: undefined,
    prices: {
      perSeat: 0,
      subscriptionMonthly: 0,
      subscriptionYearly: 0,
      perThousandMtCredits: 0,
    },
    includedUsage: {
      seats: 0,
      translations: 0,
      mtCredits: 0,
      keys: 0,
    },
    forOrganizationIds: [],
    enabledFeatures: [],
    public: false,
    free: false,
    nonCommercial: false,
    isPayAsYouGo: true,
  };
}
