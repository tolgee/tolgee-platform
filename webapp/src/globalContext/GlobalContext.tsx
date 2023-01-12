import { useOrganizationUsageService } from './useOrganizationUsageService';
import { useInitialDataService } from './useInitialDataService';
import { globalContext } from './globalActions';
import { createProviderNew } from 'tg.fixtures/createProviderNew';
import { components } from 'tg.service/apiSchema.generated';

type OrganizationModel = components['schemas']['OrganizationModel'];
type UsageModel = components['schemas']['UsageModel'];

export const [GlobalProvider, useGlobalActions, useGlobalContext] =
  createProviderNew(() => {
    const initialData = useInitialDataService();

    const organizationUsage = useOrganizationUsageService({
      organization: initialData.data.preferredOrganization,
      enabled: Boolean(initialData.data?.serverConfiguration?.billing.enabled),
    });

    const actions = {
      updatePreferredOrganization: (
        organization: number | OrganizationModel
      ) => {
        return initialData.updatePreferredOrganization(organization);
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
