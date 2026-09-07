import { components } from 'tg.service/apiSchema.generated';
import { isLimitEnforced, ProgressItem } from './component/getProgressData';

export const getSelfHostedProgressData = ({
  usage,
}: {
  usage: components['schemas']['CurrentUsageModel'];
}) => {
  const keysProgress = new ProgressItem(
    usage.keys.included,
    usage.keys.current,
    isLimitEnforced(usage.keys.limit)
  );

  const seatsProgress = new ProgressItem(
    usage.seats.included,
    usage.seats.current,
    isLimitEnforced(usage.seats.limit)
  );

  const creditsProgress = new ProgressItem(
    usage.credits.included,
    usage.credits.current
  );

  const wordsProgress = usage.words
    ? new ProgressItem(
        usage.words.included,
        usage.words.current,
        isLimitEnforced(usage.words.limit)
      )
    : undefined;

  return {
    keysProgress,
    seatsProgress,
    creditProgress: creditsProgress,
    wordsProgress,
  };
};
