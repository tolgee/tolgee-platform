import { Redirect, useLocation } from 'react-router-dom';

import { LINKS, PARAMS } from 'tg.constants/links';
import { FullPageLoading } from 'tg.component/common/FullPageLoading';
import { usePreferredOrganizationResolution } from 'tg.globalContext/helpers';

export const PreferredOrganizationRedirect = () => {
  const location = useLocation();
  const resolution = usePreferredOrganizationResolution();

  if (resolution.status === 'resolving') {
    return <FullPageLoading />;
  }

  if (resolution.status === 'missing') {
    return <Redirect to={LINKS.COMMUNITY_PROJECTS.build()} />;
  }

  const path = new URLSearchParams(location.search).get('path');
  const target = [
    LINKS.ORGANIZATION.build({
      [PARAMS.ORGANIZATION_SLUG]: resolution.organization.slug,
    }),
    path,
  ]
    .filter(Boolean)
    .join('/');

  return <Redirect to={target} />;
};
