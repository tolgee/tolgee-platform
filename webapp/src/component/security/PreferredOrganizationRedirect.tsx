import { useEffect } from 'react';
import { useHistory, useLocation } from 'react-router-dom';

import { LINKS, PARAMS } from 'tg.constants/links';
import { usePreferredOrganization } from 'tg.globalContext/helpers';
import { BoxLoading } from 'tg.component/common/BoxLoading';

export const PreferredOrganizationRedirect = () => {
  const { preferredOrganization } = usePreferredOrganization();
  const history = useHistory();
  const location = useLocation();

  useEffect(() => {
    if (preferredOrganization) {
      const queryParameters = new URLSearchParams(location.search);
      const path = queryParameters.get('path');
      const fullPath = [
        LINKS.ORGANIZATION.build({
          [PARAMS.ORGANIZATION_SLUG]: preferredOrganization.slug,
        }),
        path,
      ]
        .filter(Boolean)
        .join('/');
      history.replace(fullPath);
    }
  }, [preferredOrganization]);
  return <BoxLoading />;
};
