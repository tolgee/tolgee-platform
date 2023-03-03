import { useRouteMatch } from 'react-router-dom';

import { PARAMS } from 'tg.constants/links';
import { usePreferredOrganization } from 'tg.globalContext/helpers';
import { useApiQuery } from 'tg.service/http/useQueryApi';

export const useOrganization = () => {
  const match = useRouteMatch();
  const organizationSlug = match.params[PARAMS.ORGANIZATION_SLUG];
  const { updatePreferredOrganization } = usePreferredOrganization();

  const organization = useApiQuery({
    url: '/v2/organizations/{slug}',
    method: 'get',
    path: { slug: organizationSlug },
    options: {
      onSuccess(data) {
        updatePreferredOrganization(data.id);
      },
    },
  });

  return organization.data;
};
