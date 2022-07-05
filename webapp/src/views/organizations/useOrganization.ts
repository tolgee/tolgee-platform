import { useRouteMatch } from 'react-router-dom';

import { PARAMS } from 'tg.constants/links';
import { useInitialDataDispatch } from 'tg.hooks/InitialDataProvider';
import { useApiQuery } from 'tg.service/http/useQueryApi';

export const useOrganization = () => {
  const match = useRouteMatch();
  const organizationSlug = match.params[PARAMS.ORGANIZATION_SLUG];
  const initialDataDispatch = useInitialDataDispatch();

  const organization = useApiQuery({
    url: '/v2/organizations/{slug}',
    method: 'get',
    path: { slug: organizationSlug },
    options: {
      onSuccess(data) {
        initialDataDispatch({
          type: 'UPDATE_ORGANIZATION',
          payload: data,
        });
      },
    },
  });

  return organization.data;
};
