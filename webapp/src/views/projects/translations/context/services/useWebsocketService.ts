import { useTranslationsService } from './useTranslationsService';
import { useConfig } from 'tg.globalContext/helpers';
import { useProject } from 'tg.hooks/useProject';
import { useSelector } from 'react-redux';
import { AppState } from 'tg.store/index';
import { useEffect, useRef, useState } from 'react';
import {
  Modification,
  TranslationsModifiedData,
} from 'tg.websocket-client/WebsocketClient';
import { useGlobalContext } from 'tg.globalContext/GlobalContext';

export const useWebsocketService = (
  translationService: ReturnType<typeof useTranslationsService>
) => {
  const [eventBlockers, setEventBlockers] = useState(0);
  const config = useConfig();
  const project = useProject();
  const jwtToken = useSelector(
    (state: AppState) => state.global.security.jwtToken
  );
  const client = useGlobalContext((c) => c.client);

  function updateTranslations(event: TranslationsModifiedData) {
    const translationUpdates = event.data?.translations?.map((translation) => ({
      keyId: translation.relations.key.entityId,
      language: translation.relations.language.data.tag,
      value: getModifyingObject(translation.modifications),
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

  function handleEvent(event: TranslationsModifiedData) {
    if (eventBlockers > 0) {
      eventQueue.current.push(event);
    } else {
      updateTranslations(event);
    }
  }

  // process the blocked events, when the blocker is gone
  useEffect(() => {
    if (eventBlockers <= 0) {
      eventQueue.current.forEach((e) => {
        updateTranslations(e);
      });
      eventQueue.current = [];
    }
  }, [eventBlockers]);

  const handerRef = useRef(handleEvent);
  handerRef.current = handleEvent;

  const eventQueue = useRef([] as TranslationsModifiedData[]);

  useEffect(() => {
    if (jwtToken && client) {
      return client.subscribe(
        `/projects/${project.id}/translation-data-modified`,
        (event) => {
          // arbitrary delay, so the socket event is not faster
          // than response of http request
          setTimeout(() => handerRef.current(event), 100);
        }
      );
    }
  }, [config, project, jwtToken, client]);

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
