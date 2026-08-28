type NoOrganizationRefusal = 'no-organization';

export type BillingRefusal =
  | 'billing-disabled'
  | 'billing-not-an-owner'
  | NoOrganizationRefusal;

export type ProjectCreationRefusal =
  | NoOrganizationRefusal
  | 'not-owner-or-maintainer';

export type OrganizationCreationRefusal = 'server-disallows' | 'sso';

type Refusal =
  | BillingRefusal
  | OrganizationCreationRefusal
  | ProjectCreationRefusal;

export type DisplayedRefusal = Exclude<
  Refusal,
  'billing-disabled' | 'not-owner-or-maintainer'
>;
