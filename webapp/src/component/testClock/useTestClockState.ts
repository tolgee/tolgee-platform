import { useEffect } from 'react';
import { UseQueryResult } from 'react-query';

import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import { testClockStore } from 'tg.service/useTestClock';
import { useConfig, useIsAdminOrSupporter } from 'tg.globalContext/helpers';

export type TestClockInfo = {
  currentTimestamp: number;
  forced: boolean;
};

export const useTestClockAvailable = () => {
  const config = useConfig();
  const isAdminOrSupporter = useIsAdminOrSupporter();
  // Mirrors the SUPPORTER-only gate in WebSecurityConfig.internalSecurityFilterChain.
  return (
    config.internalControllerEnabled ||
    (config.testClockEnabled && isAdminOrSupporter)
  );
};

export const useTestClockState = () => {
  const infoLoadable = useApiQuery({
    url: '/internal/time' as any,
    method: 'get',
  }) as UseQueryResult<TestClockInfo>;

  const info = infoLoadable.data;

  useEffect(() => {
    if (!info) {
      return;
    }
    testClockStore.setState({
      time: info.forced ? info.currentTimestamp : undefined,
    });
  }, [info]);

  const moveMutation = useApiMutation({
    url: '/internal/time/{dateTimeString}' as any,
    method: 'put',
    invalidatePrefix: '/internal' as any,
  });

  const resetMutation = useApiMutation({
    url: '/internal/time' as any,
    method: 'delete',
    invalidatePrefix: '/internal' as any,
  });

  const move = (timestamp: number, onSuccess?: () => void) =>
    moveMutation.mutate({ path: { dateTimeString: timestamp } } as any, {
      onSuccess,
    });

  const reset = (onSuccess?: () => void) =>
    resetMutation.mutate({} as any, { onSuccess });

  return {
    info,
    isLoading: infoLoadable.isLoading,
    move,
    reset,
    isMoving: moveMutation.isLoading,
    isResetting: resetMutation.isLoading,
  };
};
