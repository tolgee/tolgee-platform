import { ContextOrganizationModel } from 'tg.globalContext/types';
import { memberCloudSubscription } from 'tg.fixtures/organizationEntitlement';

export const chatwootOrganizationPayload = (
  organization: ContextOrganizationModel
) => {
  const subscription = memberCloudSubscription(organization);

  return {
    plan: subscription?.plan?.name || 'free',
    subscriptionStatus: subscription?.status || 'inactive',
    organizationId: organization.id,
    organizationName: organization.name,
    enabledFeatures: organization.enabledFeatures.join(', '),
    currentUserRole: organization.currentUserRole,
  };
};
