import { components } from 'tg.service/apiSchema.generated';

type UsageModel = components['schemas']['UsageModel'];

export type ProgressData = {
  translationsAvailable: number;
  translationsMax: number;
  translationsProgress: number;
  creditAvailable: number;
  creditMax: number;
  creditProgress: number;
  smallerProgress: number;
};

export const getProgressData = (usage: UsageModel): ProgressData => {
  const translationsAvailable =
    usage.translationLimit - usage.currentTranslations;
  const translationsMax = usage.translationLimit;

  const creditAvailable = usage.creditBalance + usage.extraCreditBalance;
  const translationsProgress =
    (translationsAvailable / usage.translationLimit) * 100;
  const creditMax = usage.includedMtCredits;
  const creditProgress = (creditAvailable / creditMax) * 100;

  return {
    translationsAvailable,
    translationsMax,
    translationsProgress,
    creditAvailable,
    creditMax,
    creditProgress,
    smallerProgress: Math.min(translationsProgress, creditProgress),
  };
};
