import { PrivateOrganizationModel } from 'tg.service/apiSchemaTypes.generated';

export type OrganizationRole = PrivateOrganizationModel['currentUserRole'];

export const canManageOrganization = (
  role: OrganizationRole | undefined | null,
  staffOverride: boolean
): boolean => isOwnerOrgRole(role) || staffOverride;

export const canViewOrganization = (
  role: OrganizationRole | undefined | null,
  staffOverride: boolean
): boolean => isAtLeastMemberOrgRole(role) || staffOverride;

export const isAtLeastMemberOrgRole = (
  role: OrganizationRole | undefined | null
): boolean => Boolean(role);

export const isOwnerOrgRole = (
  role: OrganizationRole | undefined | null
): boolean => role === 'OWNER';

export const isOwnerOrMaintainerOrgRole = (
  role: OrganizationRole | undefined | null
): boolean => role === 'OWNER' || role === 'MAINTAINER';
