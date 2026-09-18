import { Redirect, useLocation } from 'react-router-dom';

import { LINKS, PARAMS } from 'tg.constants/links';
import { usePreferredOrganization } from 'tg.globalContext/helpers';
import { BoxLoading } from 'tg.component/common/BoxLoading';

export const PreferredOrganizationRedirect = () => {
  const { preferredOrganization, isFetching } = usePreferredOrganization();
  const location = useLocation();

  if (isFetching) {
    return <BoxLoading />;
  }
  if (!preferredOrganization) {
    return <Redirect to={LINKS.COMMUNITY_PROJECTS.build()} />;
  }
  const path = new URLSearchParams(location.search).get('path');
  const fullPath = [
    LINKS.ORGANIZATION.build({
      [PARAMS.ORGANIZATION_SLUG]: preferredOrganization.slug,
    }),
    path,
  ]
    .filter(Boolean)
    .join('/');
  return <Redirect to={fullPath} />;
};
