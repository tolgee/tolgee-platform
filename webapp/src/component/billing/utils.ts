import { components } from 'tg.service/apiSchema.generated';

type UsageModel = components['schemas']['UsageModel'];

export type ProgressData = {
  usesSlots: boolean;
  translationsUsed: number;
  translationsMax: number;
  translationsProgress: number;
  isPayAsYouGo: boolean;
  // translationsLimit: number;
  creditUsed: number;
  creditMax: number;
  creditProgress: number;
  biggerProgress: number;
};

export const getProgressData = (usage: UsageModel): ProgressData => {
  const usesSlots = usage.translationSlotsLimit !== -1;
  const translationsUsed = usesSlots
    ? usage.currentTranslationSlots
    : usage.currentTranslations;

  const translationsMax = usage.includedTranslations;

  const translationsLimit = usage.translationsLimit;
  const translationsProgress = (translationsUsed / translationsMax) * 100;
  const isPayAsYouGo = translationsLimit > translationsMax;

  const creditMax = usage.includedMtCredits;
  const creditUsed = creditMax - usage.creditBalance;
  const creditProgress = (creditUsed / creditMax) * 100;

  return {
    usesSlots,
    translationsUsed,
    translationsMax,
    translationsProgress,
    isPayAsYouGo,
    // translationsLimit,
    creditUsed,
    creditMax,
    creditProgress,
    biggerProgress: Math.max(translationsProgress, creditProgress),
  };
};
