import {
  useEnabledFeatures,
  useFeatureFlag,
  usePreferredOrganization,
} from 'tg.globalContext/helpers';
import { useProject } from 'tg.hooks/useProject';

export const useAiReviewEnabled = () => {
  const project = useProject();
  const { preferredOrganization } = usePreferredOrganization();
  const flagEnabled = useFeatureFlag('AI_REVIEW');
  const { isEnabled } = useEnabledFeatures();
  // Flags and features come from the preferred organization, which switches
  // to the project's organization only after the project has loaded.
  const projectOrganizationLoaded =
    preferredOrganization?.id === project.organizationOwner?.id;
  return {
    visible: flagEnabled,
    organizationLoaded: projectOrganizationLoaded,
    available:
      flagEnabled && projectOrganizationLoaded && isEnabled('AI_REVIEW'),
  };
};
