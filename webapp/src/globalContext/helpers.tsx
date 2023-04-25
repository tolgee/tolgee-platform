import { components } from 'tg.service/apiSchema.generated';

import { useGlobalContext, useGlobalActions } from './GlobalContext';

export type Feature =
  components['schemas']['SelfHostedEePlanModel']['enabledFeatures'][number];

export type FeaturesSource = 'EE_LICENSE' | 'ORGANIZATION';

export const useConfig = () => useGlobalContext((v) => v.serverConfiguration);

export const useUser = () => useGlobalContext((v) => v.userInfo);

export const useIsAdmin = () =>
  useGlobalContext((v) => v.userInfo?.globalServerRole === 'ADMIN');

export const usePreferredOrganization = () => {
  const { updatePreferredOrganization } = useGlobalActions();
  const { preferredOrganization, isFetching } = useGlobalContext((v) => ({
    preferredOrganization: v.preferredOrganization!,
    isFetching: v.isFetching,
  }));

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
  const eeSubscription = useGlobalContext((c) => c.eeSubscription);

  const eeFeatures = eeSubscription?.enabledFeatures || [];

  const organizationFeatures =
    useGlobalContext((c) => c.preferredOrganization?.enabledFeatures) || [];

  let source: FeaturesSource;

  let features: Feature[] = [];
  if (eeSubscription) {
    features = eeFeatures;
    source = 'EE_LICENSE';
  } else {
    features = organizationFeatures;
    source = 'ORGANIZATION';
  }

  return {
    features,
    isEnabled(feature: Feature) {
      return features.includes(feature);
    },
    source,
  };
};
