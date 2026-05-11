import { useEnabledFeatures } from 'tg.globalContext/helpers';
import { useProject } from 'tg.hooks/useProject';

export const useQaChecksEnabled = (): boolean => {
  const { isEnabled } = useEnabledFeatures();
  const project = useProject();
  return isEnabled('QA_CHECKS') && project.useQaChecks;
};
