import { ContextOrganizationModel } from 'tg.globalContext/types';
import { isOwnerOrMaintainerOrgRole } from 'tg.fixtures/organizationRole';
import { ProjectCreationRefusal } from 'tg.fixtures/refusal';

type Params = {
  isAdmin: boolean;
  organization: ContextOrganizationModel | undefined;
};

export const canCreateProject = (params: Params): boolean =>
  projectCreationRefusal(params) === undefined;

// Mirrors OrganizationRoleService.checkUserCanCreateProject.
export const projectCreationRefusal = ({
  isAdmin,
  organization,
}: Params): ProjectCreationRefusal | undefined => {
  if (!organization) {
    return 'no-organization';
  }

  if (isAdmin || isOwnerOrMaintainerOrgRole(organization.currentUserRole)) {
    return undefined;
  }

  return 'not-owner-or-maintainer';
};
