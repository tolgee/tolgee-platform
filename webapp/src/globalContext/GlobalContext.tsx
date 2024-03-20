import { createProvider } from 'tg.fixtures/createProvider';

import { useOrganizationUsageService } from './useOrganizationUsageService';
import { useInitialDataService } from './useInitialDataService';
import { globalContext } from './globalActions';
import { useQuickStartGuideService } from './useQuickStartGuideService';
import { useAuthService } from './useAuthService';
import { useLayoutService } from './useLayoutService';
import { useWebsocketService } from './useWsClientService';
import { useState } from 'react';
import type { GlobalError } from 'tg.error/GlobalError';
import { useConfirmationDialogService } from './useConfirmationDialogService';
import { FullPageLoading } from 'tg.component/common/FullPageLoading';
import { useMessageService } from './useMessageService';

export const [GlobalContext, useGlobalActions, useGlobalContext] =
  createProvider(() => {
    const [globalError, setGlobalError] = useState<GlobalError>();
    const initialData = useInitialDataService();
    const auth = useAuthService(initialData);
    const quickStart = useQuickStartGuideService(initialData);

    const wsClient = useWebsocketService(auth.state.jwtToken);

    const organizationUsage = useOrganizationUsageService({
      organization: initialData.state?.preferredOrganization,
      enabled: Boolean(initialData.state?.serverConfiguration?.billing.enabled),
    });

    const layout = useLayoutService();
    const confirmationDialog = useConfirmationDialogService();

    const messages = useMessageService();

    const actions = {
      setGlobalError,
      ...auth.actions,
      ...quickStart.actions,
      ...initialData.actions,
      ...organizationUsage.actions,
      ...layout.actions,
      ...confirmationDialog.actions,
      ...messages.actions,
    };

    globalContext.actions = actions;

    if (!initialData.state) {
      return <FullPageLoading />;
    }

    const contextData = {
      globalError,
      auth: auth.state,
      initialData: initialData.state,
      organizationUsage: organizationUsage.state,
      layout: layout.state,
      quickStartGuide: quickStart.state,
      confirmationDialog: confirmationDialog.state,
      wsClient,
    };

    return [contextData, actions];
  });
