import { ContextOrganizationModel } from 'tg.globalContext/types';

type Params = {
  hasCommunityContributions: boolean;
  organization: ContextOrganizationModel | undefined;
};

export const offersCommunityProjects = ({
  hasCommunityContributions,
  organization,
}: Params): boolean =>
  hasCommunityContributions || Boolean(organization?.limitedView);
