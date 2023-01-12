import { useMemo, useRef } from 'react';
import { useGlobalActions } from 'tg.globalContext/GlobalContext';
import { useApiQuery } from 'tg.service/http/useQueryApi';

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
    query: dependencies,
    path: { projectId },
    content: {
      'application/json': data,
    },
    options: {
      enabled,
    },
  });

  const machine = useApiQuery({
    url: '/v2/projects/{projectId}/suggest/machine-translations',
    method: 'post',
    path: { projectId },
    // @ts-ignore add all dependencies to properly update query
    query: dependencies,
    content: {
      'application/json': data,
    },
    fetchOptions: {
      disableBadRequestHandling: true,
    },
    options: {
      // don't refetch this when not necessary, because of credits
      staleTime: Infinity,
      cacheTime: Infinity,
      enabled,
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

  const updateTranslation = (value: string) => {
    onValueUpdate(value);
  };

  const operations = {
    updateTranslation,
  };

  const operationsRef = useRef(operations);

  operationsRef.current = operations;

  return useMemo(
    () => ({
      operationsRef,
      isFetching: memory.isFetching || machine.isFetching,
      memory: enabled ? memory : undefined,
      machine: enabled ? machine : undefined,
    }),
    [
      memory.status,
      machine.status,
      memory.isFetching,
      machine.isFetching,
      memory.data,
      machine.data,
      operationsRef,
    ]
  );
};
