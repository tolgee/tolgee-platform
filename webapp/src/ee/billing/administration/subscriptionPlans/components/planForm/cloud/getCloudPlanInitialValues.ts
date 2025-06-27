import { components } from 'tg.service/billingApiSchema.generated';
import { CloudPlanFormData } from './types';

export const getCloudPlanInitialValues = (
  planData?: components['schemas']['AdministrationCloudPlanModel']
) => {
  if (planData) {
    return {
      ...planData,
      prices: {
        perThousandMtCredits: planData.prices.perThousandMtCredits ?? 0,
        perThousandTranslations: planData.prices.perThousandTranslations ?? 0,
        perSeat: planData.prices.perSeat ?? 0,
        subscriptionMonthly: planData.prices.subscriptionMonthly ?? 0,
        subscriptionYearly: planData.prices.subscriptionYearly ?? 0,
        perThousandKeys: planData.prices.perThousandKeys ?? 0,
      },
      includedUsage: {
        ...planData.includedUsage,
        translations: planData.includedUsage.translations,
      },
      archived: planData.archivedAt != null,
      newStripeProduct: planData.stripeProductId === null,
    } as CloudPlanFormData;
  }

  return {
    type: 'PAY_AS_YOU_GO',
    metricType: 'KEYS_SEATS',
    name: '',
    stripeProductId: '',
    prices: {
      perSeat: 0,
      perThousandMtCredits: 0,
      perThousandTranslations: 0,
      subscriptionMonthly: 0,
      subscriptionYearly: 0,
      perThousandKeys: 0,
    },
    includedUsage: {
      seats: 0,
      translations: 0,
      mtCredits: 0,
      keys: 0,
    },
    enabledFeatures: [],
    public: false,
    forOrganizationIds: [],
    free: false,
    nonCommercial: false,
    archived: false,
    newStripeProduct: false,
  } as CloudPlanFormData;
};
