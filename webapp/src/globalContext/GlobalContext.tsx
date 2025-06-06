import { useState } from 'react';

import { createProvider } from 'tg.fixtures/createProvider';
import type { GlobalError } from 'tg.error/GlobalError';
import { FullPageLoading } from 'tg.component/common/FullPageLoading';
import { GlobalErrorView } from 'tg.component/common/GlobalErrorView';

import { useOrganizationUsageService } from './useOrganizationUsageService';
import { useInitialDataService } from './useInitialDataService';
import { globalContext } from './globalActions';
import { useQuickStartGuideService } from './useQuickStartGuideService';
import { useAuthService } from './useAuthService';
import { useLayoutService } from './useLayoutService';
import { useWebsocketService } from './useWsClientService';
import { useConfirmationDialogService } from './useConfirmationDialogService';
import { useMessageService } from './useMessageService';
import { useUserDraggingService } from './useUserDraggingService';
import { useUrlSearchState } from 'tg.hooks/useUrlSearchState';
import { QUERY } from 'tg.constants/links';

export const [GlobalContext, useGlobalActions, useGlobalContext] =
  createProvider(() => {
    const [globalError, setGlobalError] = useState<GlobalError>();
    const initialData = useInitialDataService();
    const auth = useAuthService(initialData);
    const [aiPlayground] = useUrlSearchState(QUERY.TRANSLATIONS_AI_PLAYGROUND, {
      defaultVal: undefined,
    });
    const isEmailVerified =
      initialData.state?.userInfo?.emailAwaitingVerification === null ||
      !initialData.state?.serverConfiguration.needsEmailVerification;
    const quickStart = useQuickStartGuideService(initialData, isEmailVerified);
    const { userIsDragging } = useUserDraggingService();

    const wsClient = useWebsocketService(
      auth.state.jwtToken,
      auth.state.allowPrivate
    );
    const organizationUsage = useOrganizationUsageService({
      organization: initialData.state?.preferredOrganization,
      enabled:
        Boolean(initialData.state?.serverConfiguration?.billing.enabled) &&
        isEmailVerified,
    });

    const layout = useLayoutService({
      quickStart,
      aiPlaygroundEnabled: Boolean(aiPlayground),
    });
    const confirmationDialog = useConfirmationDialogService();

    const messages = useMessageService();

    const actions = {
      setGlobalError,
      ...auth.actions,
      ...initialData.actions,
      ...quickStart.actions,
      ...organizationUsage.actions,
      ...layout.actions,
      ...confirmationDialog.actions,
      ...messages.actions,
    };

    globalContext.actions = actions;

    const error = initialData.error || globalError;

    if (!initialData.state) {
      return error ? <GlobalErrorView error={error} /> : <FullPageLoading />;
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
      userIsDragging,
      isEmailVerified,
      aiPlaygroundEnabled: Boolean(aiPlayground),
    };

    return [contextData, actions];
  });
