import { createProvider } from 'tg.fixtures/createProvider';
import { useOrganizationUsageService } from './useOrganizationUsageService';
import { useInitialDataService } from './useInitialDataService';
import { GlobalActionType, globalDispatchRef } from './globalActions';

export const [GlobalProvider, useGlobalDispatch, useGlobalContext] =
  createProvider(() => {
    const initialData = useInitialDataService();

    const organizationUsage = useOrganizationUsageService({
      organization: initialData.data.preferredOrganization,
      enabled: Boolean(initialData.data?.serverConfiguration?.billing.enabled),
    });

    const dispatch = async (action: GlobalActionType) => {
      switch (action.type) {
        case 'UPDATE_ORGANIZATION':
          return initialData.updatePreferredOrganization(action.payload);
        case 'REFETCH_INITIAL_DATA':
          return initialData.refetchInitialData();
        case 'REFETCH_USAGE':
          return organizationUsage.refetch();
        case 'UPDATE_USAGE':
          return organizationUsage.updateData(action.payload);
        case 'INCREMENT_PLAN_LIMIT_ERRORS':
          return organizationUsage.incrementPlanLimitErrors();
        case 'INCREMENT_NO_CREDIT_ERRORS':
          return organizationUsage.incrementNoCreditErrors();
      }
    };

    globalDispatchRef.current = dispatch;

    const contextData = {
      ...initialData.data!,
      isFetching: initialData.isFetching,
      isLoading: initialData.isLoading,
      organizationUsage: organizationUsage.data,
    };

    return [contextData, dispatch];
  });
