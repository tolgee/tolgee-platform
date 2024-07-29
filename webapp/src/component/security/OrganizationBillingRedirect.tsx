import { useEffect } from 'react';
import { useHistory } from 'react-router-dom';

import { LINKS, PARAMS } from 'tg.constants/links';
import { usePreferredOrganization } from 'tg.globalContext/helpers';
import { FullPageLoading } from 'tg.component/common/FullPageLoading';

type Props = {
  selfHosted: boolean;
};

export const OrganizationBillingRedirect = ({ selfHosted }: Props) => {
  const { preferredOrganization } = usePreferredOrganization();
  const history = useHistory();

  useEffect(() => {
    if (preferredOrganization) {
      if (selfHosted) {
        history.replace(
          LINKS.ORGANIZATION_SUBSCRIPTIONS_SELF_HOSTED_EE.build({
            [PARAMS.ORGANIZATION_SLUG]: preferredOrganization.slug,
          })
        );
      } else {
        history.replace(
          LINKS.ORGANIZATION_BILLING.build({
            [PARAMS.ORGANIZATION_SLUG]: preferredOrganization.slug,
          })
        );
      }
    }
  }, [preferredOrganization, selfHosted]);
  return <FullPageLoading />;
};
