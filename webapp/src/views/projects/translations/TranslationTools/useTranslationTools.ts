import { useMemo, useRef } from 'react';

import { stringHash } from 'tg.fixtures/stringHash';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { useTranslationsSelector } from '../context/TranslationsContext';
import { useMTStreamed } from './useMTStreamed';

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
  const contextPresent = useTranslationsSelector(
    (c) => c.translations?.find((item) => item.keyId === keyId)?.contextPresent
  );

  const dependencies = {
    keyId,
    targetLanguageId,
    baseText,
  };

  const dependenciesHash = stringHash(JSON.stringify(dependencies));

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
    query: { hash: dependenciesHash },
    path: { projectId },
    content: {
      'application/json': data,
    },
    options: {
      enabled,
    },
  });

  const machine = useMTStreamed({
    path: { projectId },
    content: { 'application/json': { ...data } },
    // @ts-ignore add all dependencies to properly update query
    query: { hash: dependenciesHash },
    fetchOptions: {
      // error is displayed inside the popup
      disableAutoErrorHandle: false,
    },
    options: {
      keepPreviousData: true,
      enabled: enabled,
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
      contextPresent,
    }),
    [
      memory.status,
      memory.isFetching,
      memory.data,
      operationsRef,
      machine.status,
      machine.isFetching,
      machine.data,
      machine.dataUpdatedAt,
    ]
  );
};
