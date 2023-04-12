import { FunctionComponent, useEffect } from 'react';
import { useHistory, useLocation, useRouteMatch } from 'react-router-dom';
import { LINKS, PARAMS } from 'tg.constants/links';

export const OrganizationBillingView: FunctionComponent = () => {
  const match = useRouteMatch();
  const organizationSlug = match.params[PARAMS.ORGANIZATION_SLUG];
  const { search } = useLocation();
  const history = useHistory();

  useEffect(() => {
    history.replace(
      LINKS.ORGANIZATION_SUBSCRIPTIONS.build({
        [PARAMS.ORGANIZATION_SLUG]: organizationSlug,
      }) + search
    );
  }, []);

  return null;
};
