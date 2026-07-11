import { components } from 'tg.service/apiSchema.generated';
import { BILLING_CRITICAL_FRACTION } from './constants';

type UsageModel = components['schemas']['PublicUsageModel'];

export const getProgressData = ({ usage }: { usage: UsageModel }) => {
  const stringsProgress = new ProgressItem(
    usage.includedTranslations,
    usage.currentTranslations
  );

  const keysProgress = new ProgressItem(usage.includedKeys, usage.currentKeys);

  const seatsProgress = new ProgressItem(
    usage.includedSeats,
    usage.currentSeats
  );

  const creditProgress = new ProgressItem(
    usage.includedMtCredits,
    usage.usedMtCredits
  );

  const mostCriticalProgress = Math.max(
    creditProgress.progress,
    stringsProgress.progress,
    keysProgress.progress,
    seatsProgress.progress
  );

  const isCritical =
    !usage.isPayAsYouGo &&
    Number(mostCriticalProgress) > BILLING_CRITICAL_FRACTION;

  return {
    stringsProgress,
    keysProgress,
    seatsProgress,
    creditProgress,
    mostCriticalProgress,
    isCritical,
  };
};

export type ProgressData = ReturnType<typeof getProgressData>;

export class ProgressItem {
  isInUse: boolean;

  constructor(public included: number, public used: number) {
    this.isInUse = included > 0;
  }

  get progress() {
    if (!this.isInUse) {
      return 0;
    }
    return this.used / this.included;
  }
}
