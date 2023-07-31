import { useOrganizationUsageService } from './useOrganizationUsageService';
import { useInitialDataService } from './useInitialDataService';
import { globalContext } from './globalActions';
import { createProvider } from 'tg.fixtures/createProvider';
import { components } from 'tg.service/apiSchema.generated';

type UsageModel = components['schemas']['PublicUsageModel'];

export const [GlobalProvider, useGlobalActions, useGlobalContext] =
  createProvider(() => {
    const initialData = useInitialDataService();

    const organizationUsage = useOrganizationUsageService({
      organization: initialData.data.preferredOrganization,
      enabled: Boolean(initialData.data?.serverConfiguration?.billing.enabled),
    });

    const actions = {
      updatePreferredOrganization: (organizationId: number) => {
        return initialData.updatePreferredOrganization(organizationId);
      },
      refetchInitialData: () => {
        return initialData.refetchInitialData();
      },
      refetchUsage: () => {
        return organizationUsage.refetch();
      },
      updateUsage: (usage: Partial<UsageModel>) => {
        return organizationUsage.updateData(usage);
      },
      incrementPlanLimitErrors: () => {
        return organizationUsage.incrementPlanLimitErrors();
      },
      incrementSpendingLimitErrors: () => {
        return organizationUsage.incrementSpendingLimitErrors();
      },
    };

    globalContext.actions = actions;

    const contextData = {
      ...initialData.data!,
      isFetching: initialData.isFetching,
      isLoading: initialData.isLoading,
      organizationUsage: organizationUsage.data,
    };

    return [contextData, actions];
  });
