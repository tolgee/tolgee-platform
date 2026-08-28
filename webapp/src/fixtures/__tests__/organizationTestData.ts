import { ContextOrganizationModel } from 'tg.globalContext/types';

export const organization = (
  data: Partial<ContextOrganizationModel> = {}
): ContextOrganizationModel => ({
  id: 1,
  name: 'Org',
  slug: 'org',
  basePermissions: { scopes: [] },
  enabledFeatures: [],
  limitedView: false,
  currentUserRole: 'MEMBER',
  ...data,
});
