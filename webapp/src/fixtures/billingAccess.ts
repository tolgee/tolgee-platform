import { ContextOrganizationModel } from 'tg.globalContext/types';
import { isOwnerOrgRole } from 'tg.fixtures/organizationRole';
import { BillingRefusal } from 'tg.fixtures/refusal';

type Params = {
  billingEnabled: boolean;
  isAdminOrSupporter: boolean;
  organization: ContextOrganizationModel | undefined;
};

export const canSeeBilling = (params: Params): boolean =>
  billingRefusal(params) === undefined;

export const billingRefusal = ({
  billingEnabled,
  isAdminOrSupporter,
  organization,
}: Params): BillingRefusal | undefined => {
  if (!billingEnabled) {
    return 'billing-disabled';
  }

  if (!organization) {
    return 'no-organization';
  }

  if (isAdminOrSupporter || isOwnerOrgRole(organization.currentUserRole)) {
    return undefined;
  }

  return 'billing-not-an-owner';
};
