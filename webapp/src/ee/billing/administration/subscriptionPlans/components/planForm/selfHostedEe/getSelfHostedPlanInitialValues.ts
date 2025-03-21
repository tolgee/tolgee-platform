import { SelfHostedEePlanFormData } from '../cloud/types';
import { components } from 'tg.service/billingApiSchema.generated';

export function getSelfHostedPlanInitialValues(
  planData?: components['schemas']['SelfHostedEePlanAdministrationModel']
): SelfHostedEePlanFormData {
  if (planData) {
    return {
      name: planData.name,
      stripeProductId: planData.stripeProductId,
      prices: planData.prices,
      includedUsage: planData.includedUsage,
      forOrganizationIds: [],
      enabledFeatures: planData.enabledFeatures,
      public: planData.public,
      free: planData.free,
      nonCommercial: planData.nonCommercial,
      isPayAsYouGo: planData.isPayAsYouGo,
    };
  }

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
