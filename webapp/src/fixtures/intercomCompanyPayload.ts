import { ContextOrganizationModel } from 'tg.globalContext/types';
import { memberCloudSubscription } from 'tg.fixtures/organizationEntitlement';

export const intercomCompanyPayload = (
  organization: ContextOrganizationModel | undefined
) => {
  const subscription = memberCloudSubscription(organization);

  if (!organization || !subscription) {
    return undefined;
  }

  return {
    company_id: organization.id,
    name: organization.name,
    plan: subscription.plan.name,
    subscriptionStatus: subscription.status,
    enabledFeatures: organization.enabledFeatures.join(', '),
  };
};
