import { useEffect, useState } from 'react';
import { useSelector } from 'react-redux';
import { createProvider } from 'tg.fixtures/createProvider';
import { components } from 'tg.service/apiSchema.generated';
import { AppState } from 'tg.store/index';
import { WebsocketClient } from 'tg.websocket-client/WebsocketClient';

import { useOrganizationUsageService } from './useOrganizationUsageService';
import { useInitialDataService } from './useInitialDataService';
import { globalContext } from './globalActions';
import { useQuickStartGuide } from './useQuickStartGuide';

type UsageModel = components['schemas']['PublicUsageModel'];

export const [GlobalProvider, useGlobalActions, useGlobalContext] =
  createProvider(() => {
    const [clientConnected, setClientConnected] = useState<boolean>();
    const [client, setClient] = useState<ReturnType<typeof WebsocketClient>>();
    const initialData = useInitialDataService();
    const [topBannerHeight, setTopBannerHeight] = useState(0);
    const [rightPanelWidth, setRightPanelWidth] = useState(0);
    const [topBarHidden, setTopBarHidden] = useState(false);
    const [quickStartState, quickStartActions] =
      useQuickStartGuide(initialData);

    const jwtToken = useSelector(
      (state: AppState) => state.global.security.jwtToken
    );

    useEffect(() => {
      if (jwtToken) {
        const newClient = WebsocketClient({
          authentication: { jwtToken: jwtToken },
          serverUrl: process.env.REACT_APP_API_URL,
          onConnected: () => setClientConnected(true),
          onConnectionClose: () => setClientConnected(false),
        });
        setClient(newClient);
        return () => {
          newClient.disconnect();
        };
      }
    }, [jwtToken]);

    const organizationUsage = useOrganizationUsageService({
      organization: initialData.data.preferredOrganization,
      enabled: Boolean(initialData.data?.serverConfiguration?.billing.enabled),
    });

    const actions = {
      ...quickStartActions,
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
      setTopBannerHeight,
      dismissTopBanner: () => {
        return initialData.dismissAnnouncement();
      },
      setRightPanelWidth,
      setTopBarHidden,
    };

    globalContext.actions = actions;

    const contextData = {
      ...initialData.data!,
      isFetching: initialData.isFetching,
      isLoading: initialData.isLoading,
      organizationUsage: organizationUsage.data,
      client,
      clientConnected,
      topBannerHeight,
      rightPanelWidth,
      topBarHeight: topBarHidden ? 0 : 52,
      quickStartGuide: quickStartState,
    };

    return [contextData, actions];
  });
