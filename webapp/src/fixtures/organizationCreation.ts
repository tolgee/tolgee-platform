import { PrivateUserAccountModel } from 'tg.service/apiSchemaTypes.generated';
import { OrganizationCreationRefusal } from 'tg.fixtures/refusal';

type ThirdPartyAuthType = PrivateUserAccountModel['thirdPartyAuthType'];

type Params = {
  isAdmin: boolean;
  thirdPartyAuthType: ThirdPartyAuthType;
  userCanCreateOrganizations: boolean;
};

export const canCreateOrganization = (params: Params): boolean =>
  organizationCreationRefusal(params) === undefined;

// Mirrors OrganizationService.organizationCreationRefusal.
export const organizationCreationRefusal = ({
  isAdmin,
  thirdPartyAuthType,
  userCanCreateOrganizations,
}: Params): OrganizationCreationRefusal | undefined => {
  if (isAdmin) {
    return undefined;
  }

  if (!userCanCreateOrganizations) {
    return 'server-disallows';
  }

  if (thirdPartyAuthType === 'SSO') {
    return 'sso';
  }

  return undefined;
};
