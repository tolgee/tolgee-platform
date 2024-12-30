import { components } from 'tg.service/billingApiSchema.generated';
import { CloudPlanFormData } from './CloudPlanFormBase';

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
      },
      includedUsage: {
        seats: planData.includedUsage.seats,
        mtCredits: planData.includedUsage.mtCredits,
        translations:
          planData.type === 'SLOTS_FIXED'
            ? planData.includedUsage.translationSlots
            : planData.includedUsage.translations,
      },
    };
  }

  return {
    type: 'PAY_AS_YOU_GO',
    name: '',
    stripeProductId: '',
    prices: {
      perSeat: 0,
      perThousandMtCredits: 0,
      perThousandTranslations: 0,
      subscriptionMonthly: 0,
      subscriptionYearly: 0,
    },
    includedUsage: {
      seats: 0,
      translations: 0,
      mtCredits: 0,
    },
    enabledFeatures: [],
    public: false,
    forOrganizationIds: [],
    free: false,
    nonCommercial: false,
  } as CloudPlanFormData;
};
