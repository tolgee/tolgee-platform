import { useEffect, useMemo, useRef } from 'react';
import { stringHash } from 'tg.fixtures/stringHash';
import { useGlobalActions } from 'tg.globalContext/GlobalContext';
import { useProjectContext } from 'tg.hooks/useProject';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { useTranslationsSelector } from '../context/TranslationsContext';

type Props = {
  projectId: number;
  keyId: number;
  targetLanguageId: number;
  baseText: string | undefined;
  enabled?: boolean;
  onValueUpdate: (value: string) => void;
};

export const useTranslationTools = ({
  projectId,
  keyId,
  baseText,
  targetLanguageId,
  onValueUpdate,
  enabled = true,
}: Props) => {
  const { updateUsage } = useGlobalActions();

  const contextPresent = useTranslationsSelector(
    (c) => c.translations?.find((item) => item.keyId === keyId)?.contextPresent
  );

  const { enabledMtServices, refetchSettings } = useProjectContext();

  const mtServices = useMemo(() => {
    const settingItem =
      enabledMtServices?.find((i) => i.targetLanguageId === targetLanguageId) ||
      enabledMtServices?.find((i) => i.targetLanguageId === null);

    return settingItem?.enabledServices || [];
  }, [enabledMtServices, targetLanguageId]);

  const fast = mtServices.filter((item) => item !== 'TOLGEE');
  const slow = mtServices.filter((item) => item === 'TOLGEE');

  const dependencies = {
    keyId,
    targetLanguageId,
    baseText,
  };

  const data = {
    keyId,
    targetLanguageId,
    // if there is not keyId, send base text, to be used for search
    baseText: keyId === undefined ? baseText : undefined,
  };

  const memory = useApiQuery({
    url: '/v2/projects/{projectId}/suggest/translation-memory',
    method: 'post',
    // @ts-ignore add all dependencies to properly update query
    query: { hash: stringHash(JSON.stringify(dependencies)) },
    path: { projectId },
    content: {
      'application/json': data,
    },
    options: {
      enabled,
    },
  });

  const machineFast = useApiQuery({
    url: '/v2/projects/{projectId}/suggest/machine-translations',
    method: 'post',
    path: { projectId },
    // @ts-ignore add all dependencies to properly update query
    query: { hash: stringHash(JSON.stringify({ ...dependencies, fast })) },
    content: {
      'application/json': { ...data, services: fast },
    },
    fetchOptions: {
      disableBadRequestHandling: true,
    },
    options: {
      keepPreviousData: true,
      enabled: enabled && Boolean(fast.length),
      onSettled(data) {
        if (data) {
          updateUsage({
            creditBalance: data.translationCreditsBalanceAfter,
            extraCreditBalance: data.translationExtraCreditsBalanceAfter,
          });
        }
      },
    },
  });

  const machineSlow = useApiQuery({
    url: '/v2/projects/{projectId}/suggest/machine-translations',
    method: 'post',
    path: { projectId },
    // @ts-ignore add all dependencies to properly update query
    query: { hash: stringHash(JSON.stringify({ ...dependencies, slow })) },
    content: {
      'application/json': { ...data, services: slow },
    },
    fetchOptions: {
      disableBadRequestHandling: true,
    },
    options: {
      keepPreviousData: true,
      refetchOnMount: true,
      enabled: enabled && Boolean(slow.length),
      onSettled(data) {
        if (data) {
          updateUsage({
            creditBalance: data.translationCreditsBalanceAfter,
            extraCreditBalance: data.translationExtraCreditsBalanceAfter,
          });
        }
      },
    },
  });

  const machine = mtServices.map((provider) => {
    const sourceData = slow.includes(provider) ? machineSlow : machineFast;
    return {
      provider,
      data: sourceData.data?.result?.[provider],
    };
  });

  const updateTranslation = (value: string) => {
    onValueUpdate(value);
  };

  const operations = {
    updateTranslation,
  };

  const operationsRef = useRef(operations);

  operationsRef.current = operations;

  useEffect(() => {
    if (
      [machineFast.error?.code, machineSlow.error?.code].includes(
        'mt_service_not_enabled'
      )
    ) {
      // refetch project mt settings if this error appears
      refetchSettings();
    }
  }, [machineFast.error?.code, machineSlow.error?.code]);

  return useMemo(
    () => ({
      operationsRef,
      isFetching:
        memory.isFetching || machineFast.isFetching || machineSlow.isFetching,
      memory: enabled ? memory : undefined,
      machine: enabled ? machine : undefined,
      machineError: machineFast.error || machineSlow.error,
      contextPresent,
    }),
    [
      memory.status,
      memory.isFetching,
      memory.data,
      operationsRef,
      machineFast.isFetching,
      machineFast.data,
      machineSlow.isFetching,
      machineSlow.data,
    ]
  );
};
