import { useGlobalContext, useGlobalActions } from './GlobalContext';

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
