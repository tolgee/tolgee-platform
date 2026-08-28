import { Feature } from 'tg.service/apiSchemaTypes';
import { ContextOrganizationModel } from 'tg.globalContext/types';
import { isAtLeastMemberOrgRole } from 'tg.fixtures/organizationRole';

export const memberIsEntitledTo = (
  organization: ContextOrganizationModel | undefined,
  feature: Feature
): boolean =>
  isAtLeastMemberOrgRole(organization?.currentUserRole) &&
  organizationOwnedFeatures(organization).includes(feature);

export const memberCloudSubscription = (
  organization: ContextOrganizationModel | undefined
) =>
  isAtLeastMemberOrgRole(organization?.currentUserRole)
    ? organization?.activeCloudSubscription
    : undefined;

const NO_FEATURES: Feature[] = [];

export const organizationOwnedFeatures = (
  organization: ContextOrganizationModel | undefined
): Feature[] => organization?.enabledFeatures ?? NO_FEATURES;
