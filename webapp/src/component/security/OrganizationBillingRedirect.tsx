import { Redirect } from 'react-router-dom';

import { LINKS, PARAMS } from 'tg.constants/links';
import { usePreferredOrganization } from 'tg.globalContext/helpers';
import { FullPageLoading } from 'tg.component/common/FullPageLoading';

type Props = {
  selfHosted: boolean;
};

export const OrganizationBillingRedirect = ({ selfHosted }: Props) => {
  const { preferredOrganization, isFetching } = usePreferredOrganization();

  if (isFetching) {
    return <FullPageLoading />;
  }
  if (!preferredOrganization) {
    return <Redirect to={LINKS.COMMUNITY_PROJECTS.build()} />;
  }
  const target = selfHosted
    ? LINKS.ORGANIZATION_SUBSCRIPTIONS_SELF_HOSTED_EE
    : LINKS.ORGANIZATION_BILLING;
  return (
    <Redirect
      to={target.build({
        [PARAMS.ORGANIZATION_SLUG]: preferredOrganization.slug,
      })}
    />
  );
};
