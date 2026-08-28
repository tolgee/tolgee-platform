import { Redirect } from 'react-router-dom';

import { LINKS, PARAMS } from 'tg.constants/links';
import {
  canManageOrganization,
  canViewOrganization,
} from 'tg.fixtures/organizationRole';
import { useIsAdminOrSupporter } from 'tg.globalContext/helpers';
import { useOrganization } from 'tg.views/organizations/useOrganization';

type Level = 'member' | 'manage';

type Props = {
  level: Level;
};

export const RequireOrganizationAccess: React.FC<
  React.PropsWithChildren<Props>
> = ({ level, children }) => {
  const organization = useOrganization();
  const isAdminOrSupporter = useIsAdminOrSupporter();

  if (!organization) {
    return null;
  }

  const role = organization.currentUserRole;
  const permitted =
    level === 'manage'
      ? canManageOrganization(role, isAdminOrSupporter)
      : canViewOrganization(role, isAdminOrSupporter);

  if (!permitted) {
    return (
      <Redirect
        to={LINKS.ORGANIZATION_PROFILE.build({
          [PARAMS.ORGANIZATION_SLUG]: organization.slug,
        })}
      />
    );
  }

  return <>{children}</>;
};
