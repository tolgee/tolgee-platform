import { useMemo, useRef } from 'react';
import { useOrganizationUsageMethods } from 'tg.globalContext/helpers';
import { useApiQuery } from 'tg.service/http/useQueryApi';

type Props = {
  projectId: number;
  keyId: number;
  targetLanguageId: number;
  baseText?: string;
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
  const { updateUsage } = useOrganizationUsageMethods();
  const memory = useApiQuery({
    url: '/v2/projects/{projectId}/suggest/translation-memory',
    method: 'post',
    query: {
      // @ts-ignore add keyId to url to reset data when key changes
      keyId,
      targetLanguageId,
      baseText,
    },
    path: { projectId },
    content: { 'application/json': { keyId, targetLanguageId, baseText } },
    options: {
      enabled,
    },
  });

  const machine = useApiQuery({
    url: '/v2/projects/{projectId}/suggest/machine-translations',
    method: 'post',
    path: { projectId },
    // @ts-ignore add keyId to url to reset data when key changes
    query: {
      keyId,
    },
    content: {
      'application/json': { keyId: keyId, targetLanguageId, baseText },
    },
    options: {
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
