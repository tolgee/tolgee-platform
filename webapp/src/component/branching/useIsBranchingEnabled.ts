import { useProjectContextOptional } from 'tg.hooks/useProject';
import { useEnabledFeatures } from 'tg.globalContext/helpers';

export const useIsBranchingEnabled = () => {
  const project = useProjectContextOptional()?.project; // useProject() throws error here, because project may not be loaded
  const { isEnabled } = useEnabledFeatures();

  return isEnabled('BRANCHING') && !!project?.useBranching;
};
