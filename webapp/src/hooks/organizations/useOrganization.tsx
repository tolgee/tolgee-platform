import {useRouteMatch} from 'react-router-dom';
import {container} from 'tsyringe';
import {useEffect} from 'react';
import {OrganizationActions} from '../../store/organization/OrganizationActions';
import {PARAMS} from '../../constants/links';

const actions = container.resolve(OrganizationActions);

export const useOrganization = () => {
  const match = useRouteMatch();

  const organizationSlug =
    match.params[PARAMS.ORGANIZATION_SLUG];
  const resourceLoadable = actions.useSelector((state) => state.loadables.get);

  useEffect(() => {
    if (
      !resourceLoadable.touched ||
      resourceLoadable.data?.slug !== organizationSlug
    ) {
      actions.loadableActions.get.dispatch(organizationSlug);
    }
  }, [organizationSlug, resourceLoadable.touched]);

  return resourceLoadable.data as NonNullable<typeof resourceLoadable.data>;
};
