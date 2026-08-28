import { useRouteMatch } from 'react-router-dom';

import { PARAMS } from 'tg.constants/links';
import { useApiQuery } from 'tg.service/http/useQueryApi';

export const useOrganization = () => useOrganizationLoadable().data;

export const useOrganizationLoadable = () => {
  const match = useRouteMatch();
  const organizationSlug = match.params[PARAMS.ORGANIZATION_SLUG];

  return useApiQuery({
    url: '/v2/organizations/{slug}',
    method: 'get',
    path: { slug: organizationSlug },
  });
};
