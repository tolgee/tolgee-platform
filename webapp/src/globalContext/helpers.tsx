import { components } from 'tg.service/apiSchema.generated';

import { useGlobalActions, useGlobalContext } from './GlobalContext';

export type Feature =
  components['schemas']['SelfHostedEePlanModel']['enabledFeatures'][number];

export type FeaturesSource = 'EE_LICENSE' | 'ORGANIZATION';

export const useConfig = () =>
  useGlobalContext((c) => c.initialData.serverConfiguration);

export const useUser = () => useGlobalContext((c) => c.initialData.userInfo);

export const useIsEmailVerified = () =>
  useGlobalContext((c) => c.isEmailVerified);

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
  const isFetching = useGlobalContext((c) => c.initialData.isFetching);

  return {
    preferredOrganization,
    updatePreferredOrganization,
    isFetching,
  };
};

export const useOrganizationUsage = () => {
  return useGlobalContext((v) => v.organizationUsage!);
};

export const useEnabledFeatures = () => {
  const features =
    useGlobalContext(
      (c) => c.initialData.preferredOrganization?.enabledFeatures
    ) || [];

  return {
    features,
    isEnabled(feature: Feature) {
      return features.includes(feature);
    },
  };
};
