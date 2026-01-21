import { useProject } from 'tg.hooks/useProject';
import { useEnabledFeatures } from 'tg.globalContext/helpers';

export const useIsBranchingEnabled = () => {
  const project = useProject();
  const { isEnabled } = useEnabledFeatures();

  return isEnabled('BRANCHING') && project.useBranching;
};
