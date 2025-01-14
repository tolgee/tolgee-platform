import { components } from 'tg.service/apiSchema.generated';
import { BILLING_CRITICAL_FRACTION } from './constants';

type UsageModel = components['schemas']['PublicUsageModel'];

export type ProgressData = {
  usesSlots: boolean;
  translationsUsed: number;
  translationsMax: number;
  translationsProgress: number;
  isPayAsYouGo: boolean;
  creditUsed: number;
  creditMax: number;
  creditProgress: number;
  moreCriticalProgress: number;
  isCritical: boolean;
};

export const getProgressData = (usage: UsageModel): ProgressData => {
  const usesSlots = usage.translationSlotsLimit !== -1;
  const translationsUsed = usesSlots
    ? usage.currentTranslationSlots
    : usage.currentTranslations;

  const translationsMax = usesSlots
    ? usage.includedTranslationSlots
    : usage.includedTranslations;

  const translationsLimit = usesSlots
    ? usage.translationSlotsLimit
    : usage.translationsLimit;
  const translationsProgress = translationsUsed / translationsMax;
  const isPayAsYouGo = translationsLimit > translationsMax;

  const creditMax = usage.includedMtCredits;
  const creditUsed =
    creditMax - usage.creditBalance + usage.currentPayAsYouGoMtCredits;

  const creditProgress = creditUsed / creditMax;

  const creditProgressWithExtraUnnormalized =
    (creditUsed - usage.extraCreditBalance) / creditMax;

  const creditProgressExtra =
    creditProgressWithExtraUnnormalized <= 0
      ? 0
      : creditProgressWithExtraUnnormalized;

  const moreCriticalProgress = Math.max(
    translationsProgress,
    creditProgressExtra
  );

  const isCritical =
    !isPayAsYouGo && Number(moreCriticalProgress) > BILLING_CRITICAL_FRACTION;

  return {
    usesSlots,
    translationsUsed,
    translationsMax,
    translationsProgress,
    isPayAsYouGo,
    creditUsed,
    creditMax,
    creditProgress,
    moreCriticalProgress,
    isCritical,
  };
};
