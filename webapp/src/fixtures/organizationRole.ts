import { components } from 'tg.service/apiSchema.generated';

type OrganizationRole =
  components['schemas']['PrivateOrganizationModel']['currentUserRole'];

export const isAtLeastMemberOrgRole = (
  role: OrganizationRole | undefined | null
): boolean => Boolean(role);
