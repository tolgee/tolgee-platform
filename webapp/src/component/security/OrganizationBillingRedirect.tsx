import { Redirect } from 'react-router-dom';

import { LINKS } from 'tg.constants/links';
import { FullPageLoading } from 'tg.component/common/FullPageLoading';
import {
  useBillingRefusal,
  usePreferredOrganizationResolution,
} from 'tg.globalContext/helpers';
import { NoPermissionsView } from 'tg.component/common/NoPermissionsView';
import { billingLinkFor } from 'tg.fixtures/billingLink';

type Props = {
  selfHosted: boolean;
};

export const OrganizationBillingRedirect = ({ selfHosted }: Props) => {
  const resolution = usePreferredOrganizationResolution();
  const refusal = useBillingRefusal();

  if (resolution.status === 'resolving') {
    return <FullPageLoading />;
  }

  if (resolution.status === 'missing') {
    return <Redirect to={LINKS.COMMUNITY_PROJECTS.build()} />;
  }

  if (refusal === 'billing-disabled') {
    return <Redirect to={LINKS.PROJECTS.build()} />;
  }

  if (refusal) {
    return <NoPermissionsView reason={refusal} />;
  }

  return (
    <Redirect
      to={billingLinkFor({ slug: resolution.organization.slug, selfHosted })}
    />
  );
};
