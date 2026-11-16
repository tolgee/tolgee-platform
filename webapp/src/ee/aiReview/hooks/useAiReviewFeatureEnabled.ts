import { useEnabledFeatures } from 'tg.globalContext/helpers';

export const useAiReviewFeatureEnabled = () => {
  const { isEnabled } = useEnabledFeatures();
  return isEnabled('AI_REVIEW');
};
