import { ContextOrganizationModel } from 'tg.globalContext/types';

type Params = {
  organization: ContextOrganizationModel | undefined;
  isSwitching: boolean;
};

export type PreferredOrganizationResolution =
  | { status: 'resolving' }
  | { status: 'missing' }
  | { status: 'resolved'; organization: ContextOrganizationModel };

export const preferredOrganizationResolution = ({
  organization,
  isSwitching,
}: Params): PreferredOrganizationResolution => {
  if (organization) {
    return { status: 'resolved', organization };
  }

  if (isSwitching) {
    return { status: 'resolving' };
  }

  return { status: 'missing' };
};
