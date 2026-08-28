import { Feature, QaCheckType } from 'tg.service/apiSchemaTypes';
import { QaCheckCategoryModel } from 'tg.service/apiSchemaTypes.generated';
import { organizationHasSupportChat } from 'tg.fixtures/supportChat';
import {
  canCreateOrganization,
  organizationCreationRefusal,
} from 'tg.fixtures/organizationCreation';
import { preferredOrganizationResolution } from 'tg.fixtures/preferredOrganizationResolution';
import {
  canCreateProject,
  projectCreationRefusal,
} from 'tg.fixtures/projectCreation';
import { billingRefusal, canSeeBilling } from 'tg.fixtures/billingAccess';
import { organizationOwnedFeatures } from 'tg.fixtures/organizationEntitlement';
import { isOwnerOrMaintainerOrgRole } from 'tg.fixtures/organizationRole';

import { useGlobalActions, useGlobalContext } from './GlobalContext';

export const useConfig = () =>
  useGlobalContext((c) => c.initialData.serverConfiguration);

export const useUser = () => useGlobalContext((c) => c.initialData.userInfo);

export const useIsEmailVerified = () =>
  useGlobalContext((c) => c.isEmailVerified);

export const useHasCommunityContributions = () =>
  useGlobalContext((c) => c.initialData.hasCommunityContributions);

export const useEmailAwaitingVerification = () =>
  useGlobalContext((c) => c.initialData.userInfo?.emailAwaitingVerification);

export const useIsAdmin = () =>
  useGlobalContext((c) => c.initialData.userInfo?.globalServerRole === 'ADMIN');

export const useIsSupporter = () =>
  useGlobalContext(
    (c) => c.initialData.userInfo?.globalServerRole === 'SUPPORTER'
  );

export const useIsAdminOrSupporter = () =>
  useGlobalContext((c) => {
    const role = c.initialData.userInfo?.globalServerRole;
    return role === 'ADMIN' || role === 'SUPPORTER';
  });

export const useIsBeingImpersonated = () =>
  useGlobalContext((c) => Boolean(c.auth.adminToken));

export const useIsSsoMigrationRequired = () =>
  useGlobalContext(
    (c) =>
      c.initialData.ssoInfo?.force &&
      c.initialData.userInfo?.accountType !== 'MANAGED'
  );

export const usePreferredOrganization = () => {
  const { updatePreferredOrganization } = useGlobalActions();
  const preferredOrganization = useGlobalContext(
    (c) => c.initialData.preferredOrganization
  );
  return {
    preferredOrganization,
    updatePreferredOrganization,
  };
};

export const useIsBillingEnabled = () =>
  useGlobalContext((c) => c.initialData.serverConfiguration.billing.enabled);

export const useCanSeeBilling = () => canSeeBilling(useBillingAccessParams());

export const useBillingRefusal = () => billingRefusal(useBillingAccessParams());

export const useBillingOrganization = () => {
  const params = useBillingAccessParams();
  return canSeeBilling(params) ? params.organization : undefined;
};

const useBillingAccessParams = () => {
  const billingEnabled = useIsBillingEnabled();
  const { preferredOrganization } = usePreferredOrganization();
  const isAdminOrSupporter = useIsAdminOrSupporter();

  return {
    billingEnabled,
    isAdminOrSupporter,
    organization: preferredOrganization,
  };
};

export const useIsOrganizationOwnerOrMaintainer = () => {
  const { preferredOrganization } = usePreferredOrganization();
  return isOwnerOrMaintainerOrgRole(preferredOrganization?.currentUserRole);
};

export const useCanCreateOrganization = () =>
  canCreateOrganization(useOrganizationCreationParams());

export const useOrganizationCreationRefusal = () =>
  organizationCreationRefusal(useOrganizationCreationParams());

const useOrganizationCreationParams = () => {
  const isAdmin = useIsAdmin();
  const config = useConfig();
  const user = useUser();

  return {
    isAdmin,
    thirdPartyAuthType: user?.thirdPartyAuthType,
    userCanCreateOrganizations: config.userCanCreateOrganizations,
  };
};

export const useIsSwitchingOrganization = () =>
  useGlobalContext((c) => c.initialData.isSwitchingOrganization);

export const usePreferredOrganizationResolution = () => {
  const { preferredOrganization } = usePreferredOrganization();
  const isSwitching = useIsSwitchingOrganization();

  return preferredOrganizationResolution({
    organization: preferredOrganization,
    isSwitching,
  });
};

export const useCanCreateProject = () =>
  canCreateProject(useProjectCreationParams());

export const useProjectCreationRefusal = () =>
  projectCreationRefusal(useProjectCreationParams());

const useProjectCreationParams = () => {
  const isAdmin = useIsAdmin();
  const { preferredOrganization } = usePreferredOrganization();

  return { isAdmin, organization: preferredOrganization };
};

export const useOrganizationUsage = () => {
  return useGlobalContext((v) => v.organizationUsage!);
};

export const useQaCategories = () =>
  useGlobalContext((c) => c.initialData.qaCheckCategories ?? EMPTY_LIST);

export const useQaCheckTypes = (): QaCheckType[] => {
  const categories = useQaCategories();
  return categories.flatMap((c) => c.checkTypes);
};

export const useEnabledFeatures = () => {
  const { preferredOrganization } = usePreferredOrganization();
  const features = organizationOwnedFeatures(preferredOrganization);

  return {
    features,
    isEnabled(feature: Feature) {
      return features.includes(feature);
    },
  };
};

export const useHasSupportChat = () => {
  const { preferredOrganization } = usePreferredOrganization();

  return organizationHasSupportChat(preferredOrganization);
};

const EMPTY_LIST: QaCheckCategoryModel[] = [];
