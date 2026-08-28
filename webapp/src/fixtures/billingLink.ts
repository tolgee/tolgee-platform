import { LINKS, PARAMS } from 'tg.constants/links';

type Target = {
  slug: string;
  selfHosted?: boolean;
};

// Subscriptions, not ORGANIZATION_BILLING: see OrganizationBillingView in the billing repo.
export const billingLinkFor = ({ slug, selfHosted }: Target) =>
  (selfHosted
    ? LINKS.ORGANIZATION_SUBSCRIPTIONS_SELF_HOSTED_EE
    : LINKS.ORGANIZATION_SUBSCRIPTIONS
  ).build({ [PARAMS.ORGANIZATION_SLUG]: slug });
