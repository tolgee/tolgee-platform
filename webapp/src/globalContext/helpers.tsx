import { components } from 'tg.service/apiSchema.generated';

import { useGlobalActions, useGlobalContext } from './GlobalContext';

export type Feature =
  components['schemas']['SelfHostedEePlanModel']['enabledFeatures'][number];

export type FeaturesSource = 'EE_LICENSE' | 'ORGANIZATION';

export const useConfig = () => useGlobalContext((v) => v.serverConfiguration);

export const useUser = () => useGlobalContext((v) => v.userInfo);

export const useIsAdmin = () =>
  useGlobalContext((v) => v.userInfo?.globalServerRole === 'ADMIN');

export const usePreferredOrganization = () => {
  const { updatePreferredOrganization } = useGlobalActions();
  const preferredOrganization = useGlobalContext(
    (c) => c.preferredOrganization
  );
  const isFetching = useGlobalContext((c) => c.isFetching);

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
    useGlobalContext((c) => c.preferredOrganization?.enabledFeatures) || [];

  return {
    features,
    isEnabled(feature: Feature) {
      return features.includes(feature);
    },
  };
};
