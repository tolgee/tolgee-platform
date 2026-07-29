import { PrivateOrganizationModel } from 'tg.service/apiSchemaTypes.generated';

export type OrganizationRole = PrivateOrganizationModel['currentUserRole'];

export const isAtLeastMemberOrgRole = (
  role: OrganizationRole | undefined | null
): boolean => Boolean(role);

export const isOwnerOrMaintainerOrgRole = (
  role: OrganizationRole | undefined | null
): boolean => role === 'OWNER' || role === 'MAINTAINER';
