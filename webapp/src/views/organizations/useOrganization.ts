import { useRouteMatch } from 'react-router-dom';

import { PARAMS } from 'tg.constants/links';
import { useUpdateCurrentOrganization } from 'tg.hooks/CurrentOrganizationProvider';
import { useApiQuery } from 'tg.service/http/useQueryApi';

export const useOrganization = () => {
  const match = useRouteMatch();
  const organizationSlug = match.params[PARAMS.ORGANIZATION_SLUG];
  const updateCurrentOrganization = useUpdateCurrentOrganization();

  const organization = useApiQuery({
    url: '/v2/organizations/{slug}',
    method: 'get',
    path: { slug: organizationSlug },
    options: {
      onSuccess(data) {
        updateCurrentOrganization(data);
      },
    },
  });

  return organization.data;
};
