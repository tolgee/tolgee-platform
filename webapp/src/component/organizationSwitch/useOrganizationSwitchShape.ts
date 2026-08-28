import {
  useCanCreateOrganization,
  useHasCommunityContributions,
  usePreferredOrganization,
} from 'tg.globalContext/helpers';
import { offersCommunityProjects } from 'tg.fixtures/communitySurface';
import { organizationSwitchShape } from 'tg.fixtures/organizationSwitchShape';

import { useIsCommunitySurface } from 'tg.component/organizationSwitch/useIsCommunitySurface';

export const useOrganizationSwitchShape = () => {
  const { preferredOrganization } = usePreferredOrganization();
  const hasCommunityContributions = useHasCommunityContributions();
  const canCreateOrganization = useCanCreateOrganization();
  const isCommunitySurface = useIsCommunitySurface();

  return organizationSwitchShape({
    hasPreferredOrganization: Boolean(preferredOrganization),
    isCommunitySurface,
    canCreateOrganization,
    showsCommunityProjects: offersCommunityProjects({
      hasCommunityContributions,
      organization: preferredOrganization,
    }),
  });
};
