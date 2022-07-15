import { components } from 'tg.service/apiSchema.generated';
import { useGlobalContext, useGlobalDispatch } from './GlobalContext';

type OrganizationModel = components['schemas']['OrganizationModel'];
type UsageModel = components['schemas']['UsageModel'];

export const useConfig = () => useGlobalContext((v) => v.serverConfiguration);

export const useUser = () => useGlobalContext((v) => v.userInfo);

export const usePreferredOrganization = () => {
  const initialDataDispatch = useGlobalDispatch();
  const preferredOrganization = useGlobalContext(
    (v) => v.preferredOrganization!
  );
  const updatePreferredOrganization = (org: number | OrganizationModel) =>
    initialDataDispatch({ type: 'UPDATE_ORGANIZATION', payload: org });
  return { preferredOrganization, updatePreferredOrganization };
};

export const useOrganizationUsage = () => {
  return useGlobalContext((v) => v.organizationUsage!);
};

export const useOrganizationUsageMethods = () => {
  const initialDataDispatch = useGlobalDispatch();
  const updateUsage = (data: Partial<UsageModel>) =>
    initialDataDispatch({ type: 'UPDATE_USAGE', payload: data });
  const refetchUsage = () => initialDataDispatch({ type: 'REFETCH_USAGE' });
  return { updateUsage, refetchUsage };
};
