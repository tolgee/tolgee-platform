import { useRouteMatch } from 'react-router-dom';
import { PARAMS } from '../../../constants/links';
import { useGetOrganization } from '../../../service/hooks/Organization';

export const useOrganization = () => {
  const match = useRouteMatch();
  const organizationSlug = match.params[PARAMS.ORGANIZATION_SLUG];

  const organization = useGetOrganization(organizationSlug);

  return organization.data;
};
