import { components } from 'tg.service/apiSchema.generated';
import { ProgressItem } from './component/getProgressData';

export const getSelfHostedProgressData = ({
  usage,
}: {
  usage: components['schemas']['CurrentUsageModel'];
}) => {
  const keysProgress = new ProgressItem(
    usage.keys.included,
    usage.keys.current
  );

  const seatsProgress = new ProgressItem(
    usage.seats.included,
    usage.seats.current
  );

  const creditsProgress = new ProgressItem(
    usage.credits.included,
    usage.credits.current
  );

  return {
    keysProgress,
    seatsProgress,
    creditProgress: creditsProgress,
  };
};
