import { components } from 'tg.service/apiSchema.generated';
import { BILLING_CRITICAL_FRACTION } from './constants';

type UsageModel = components['schemas']['PublicUsageModel'];

/**
 * Mirrors `UsageLimits.Limit.isEnforced` on the backend. Not `!== -1`: -2 (negotiated) is a second
 * unenforced sentinel, and treating it as a ceiling renders "of -2 words" and a nonsense ratio.
 */
export const isLimitEnforced = (limit: number) => limit >= 0;

export const getProgressData = ({ usage }: { usage: UsageModel }) => {
  const stringsProgress = new ProgressItem(
    usage.includedTranslations,
    usage.currentTranslations,
    isLimitEnforced(usage.translationsLimit)
  );

  const keysProgress = new ProgressItem(
    usage.includedKeys,
    usage.currentKeys,
    isLimitEnforced(usage.keysLimit)
  );

  const seatsProgress = new ProgressItem(
    usage.includedSeats,
    usage.currentSeats,
    isLimitEnforced(usage.seatsLimit)
  );

  const creditProgress = new ProgressItem(
    usage.includedMtCredits,
    usage.usedMtCredits
  );

  const wordsProgress = new ProgressItem(
    usage.includedWords,
    usage.currentWords,
    isLimitEnforced(usage.wordsLimit)
  );

  const mostCriticalProgress = Math.max(
    creditProgress.progress,
    stringsProgress.progress,
    keysProgress.progress,
    seatsProgress.progress,
    wordsProgress.progress
  );

  const isCritical =
    !usage.isPayAsYouGo &&
    Number(mostCriticalProgress) > BILLING_CRITICAL_FRACTION;

  return {
    stringsProgress,
    keysProgress,
    seatsProgress,
    creditProgress,
    wordsProgress,
    mostCriticalProgress,
    isCritical,
  };
};

export type ProgressData = ReturnType<typeof getProgressData>;

export class ProgressItem {
  isInUse: boolean;

  /**
   * A plan that does not charge for a metric has its limit reported as -1; a bar for the allowance
   * it still carries would pin an organization over a limit that does not exist.
   */
  constructor(public included: number, public used: number, enforced = true) {
    this.isInUse = enforced && included > 0;
  }

  get progress() {
    if (!this.isInUse) {
      return 0;
    }
    return this.used / this.included;
  }
}
