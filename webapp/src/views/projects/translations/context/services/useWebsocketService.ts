import { useTranslationsService } from './useTranslationsService';
import { useProject } from 'tg.hooks/useProject';
import { useEffect, useRef, useState } from 'react';
import {
  Modification,
  TranslationsModifiedData,
} from 'tg.websocket-client/WebsocketClient';
import { useGlobalContext } from 'tg.globalContext/GlobalContext';
import { useEnabledFeatures } from 'tg.globalContext/helpers';
import { useDebouncedCallback } from 'use-debounce';

export const useWebsocketService = (
  translationService: ReturnType<typeof useTranslationsService>
) => {
  const [eventBlockers, setEventBlockers] = useState(0);
  const project = useProject();
  const client = useGlobalContext((c) => c.wsClient.client);
  const { isEnabled } = useEnabledFeatures();
  const qaChecksEnabled = isEnabled('QA_CHECKS');

  function updateTranslations(event: TranslationsModifiedData) {
    const translationUpdates = event.data?.translations?.map((translation) => ({
      keyId: translation.relations.key.entityId,
      language: translation.relations.language.data.tag,
      value: {
        ...getModifyingObject(translation.modifications),
        id: translation.id,
      },
    }));

    if (translationUpdates) {
      translationService.changeTranslations(translationUpdates);
    }

    const keyUpdates = event.data?.keys?.map((key) => {
      const isDeleted =
        key.changeType === 'DEL' ||
        (key.modifications as Record<string, Modification<unknown>>)?.deletedAt
          ?.new != null;

      return {
        keyId: key.id,
        value: isDeleted
          ? { deleted: true }
          : {
              ...getModifyingObject(key.modifications, {
                name: 'keyName',
              }),
            },
      };
    });

    if (keyUpdates) {
      translationService.updateTranslationKeys(keyUpdates);
    }
  }

  const eventQueue = useRef([] as TranslationsModifiedData[]);

  const handleQueue = () => {
    if (eventBlockers <= 0) {
      eventQueue.current.forEach((e) => {
        updateTranslations(e);
      });
      eventQueue.current = [];
    }
  };

  // process the blocked events, when the blocker is gone
  useEffect(() => {
    handleQueue();
  }, [eventBlockers]);

  const handlerRef = useRef(handleQueue);
  handlerRef.current = handleQueue;

  const handleQueueDelayed = useDebouncedCallback(
    () => {
      handlerRef.current();
    },
    100,
    { maxWait: 100 }
  );

  useEffect(() => {
    if (client) {
      return client.subscribe(
        `/projects/${project.id}/translation-data-modified`,
        (event) => {
          // arbitrary delay, so the socket event is not faster
          // than response of http request
          eventQueue.current.push(event);
          handleQueueDelayed();
        }
      );
    }
  }, [project, client]);

  useEffect(() => {
    if (client && qaChecksEnabled) {
      return client.subscribe(
        `/projects/${project.id}/qa-issues-updated`,
        (event) => {
          translationService.changeTranslations([
            {
              keyId: event.data.keyId,
              language: event.data.languageTag,
              value: {
                id: event.data.translationId,
                qaIssueCount: event.data.qaIssueCount,
                qaChecksStale: event.data.qaChecksStale,
                qaIssues: event.data.qaIssues,
              },
            },
          ]);
        }
      );
    }
  }, [project, client, qaChecksEnabled]);

  return {
    setEventBlockers,
  };
};

const getModifyingObject = (
  value: Record<string, Modification<any>>,
  fieldMapping?: Record<string, string>
) => {
  const result: Record<string, unknown> = {};
  for (const [field, modification] of Object.entries(value)) {
    result[fieldMapping?.[field] || field] = modification.new;
  }
  return result;
};
