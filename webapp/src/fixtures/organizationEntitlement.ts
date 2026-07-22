import { components } from 'tg.service/apiSchema.generated';
import { Feature } from 'tg.service/apiSchemaTypes';

type PrivateOrganization = components['schemas']['PrivateOrganizationModel'];

export const organizationHasSupportChat = (
  organization: PrivateOrganization | undefined
): boolean =>
  isOrganizationEntitledTo(organization, 'STANDARD_SUPPORT') ||
  isOrganizationEntitledTo(organization, 'PREMIUM_SUPPORT');

export const organizationCompanyInfo = (
  organization: PrivateOrganization | undefined
) => {
  const subscription = organization?.activeCloudSubscription;

  if (!organization || !subscription) {
    return null;
  }

  return {
    company_id: organization.id,
    name: organization.name,
    plan: subscription.plan.name,
    subscriptionStatus: subscription.status,
    enabledFeatures: organization.enabledFeatures.join(', '),
  };
};

const isOrganizationEntitledTo = (
  organization: PrivateOrganization | undefined,
  feature: Feature
): boolean => {
  if (!organization || organization.limitedView) {
    return false;
  }

  return organization.enabledFeatures.includes(feature);
};
