import { useEffect } from 'react';
import { useHistory } from 'react-router-dom';

import { LINKS, PARAMS } from 'tg.constants/links';
import { usePreferredOrganization } from 'tg.globalContext/helpers';
import { FullPageLoading } from 'tg.component/common/FullPageLoading';

export const OrganizationRedirectHandler = () => {
  const { preferredOrganization } = usePreferredOrganization();
  const history = useHistory();

  useEffect(() => {
    if (preferredOrganization) {
      history.replace(
        LINKS.ORGANIZATION_BILLING.build({
          [PARAMS.ORGANIZATION_SLUG]: preferredOrganization.slug,
        })
      );
    }
  }, [preferredOrganization]);
  return <FullPageLoading />;
};
