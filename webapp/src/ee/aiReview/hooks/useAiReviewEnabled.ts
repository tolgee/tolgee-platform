import { useEnabledFeatures } from 'tg.globalContext/helpers';

export const useAiReviewEnabled = () => {
  const { isEnabled } = useEnabledFeatures();
  return isEnabled('AI_REVIEW');
};
