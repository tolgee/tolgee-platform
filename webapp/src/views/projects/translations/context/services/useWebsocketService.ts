import { useTranslationsService } from './useTranslationsService';
import { useProject } from 'tg.hooks/useProject';
import { useEffect, useRef, useState } from 'react';
import {
  Modification,
  TranslationsModifiedData,
} from 'tg.websocket-client/WebsocketClient';
import { useGlobalContext } from 'tg.globalContext/GlobalContext';
import { useDebouncedCallback } from 'use-debounce';

export const useWebsocketService = (
  translationService: ReturnType<typeof useTranslationsService>
) => {
  const [eventBlockers, setEventBlockers] = useState(0);
  const project = useProject();
  const client = useGlobalContext((c) => c.wsClient.client);

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

    const keyUpdates = event.data?.keys?.map((key) => ({
      keyId: key.id,
      value:
        key.changeType == 'DEL'
          ? { deleted: true }
          : {
              ...getModifyingObject(key.modifications, {
                name: 'keyName',
              }),
            },
    }));

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

  const handerRef = useRef(handleQueue);
  handerRef.current = handleQueue;

  const handleQueueDelayed = useDebouncedCallback(
    () => {
      handerRef.current();
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

  return {
    setEventBlockers,
  };
};

const getModifyingObject = (
  value: Record<string, Modification<any>>,
  fieldMapping?: Record<string, string>
) => {
  const result = {};
  Object.entries(value).forEach(([field, modification]) => {
    result[fieldMapping?.[field] || field] = modification.new;
  });
  return result;
};
