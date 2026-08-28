import { ContextOrganizationModel } from 'tg.globalContext/types';
import { memberIsEntitledTo } from 'tg.fixtures/organizationEntitlement';

export const organizationHasSupportChat = (
  organization: ContextOrganizationModel | undefined
): boolean =>
  memberIsEntitledTo(organization, 'STANDARD_SUPPORT') ||
  memberIsEntitledTo(organization, 'PREMIUM_SUPPORT');
