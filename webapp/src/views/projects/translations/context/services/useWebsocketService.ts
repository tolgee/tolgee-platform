import { useTranslationsService } from './useTranslationsService';
import { useProject } from 'tg.hooks/useProject';
import { useEffect, useRef, useState } from 'react';
import {
  Modification,
  TranslationsModifiedData,
  QaIssuesUpdatedData,
  AiReviewUpdatedData,
} from 'tg.websocket-client/WebsocketClient';
import { useGlobalContext } from 'tg.globalContext/GlobalContext';
import { useAiReviewEnabled, useQaChecksEnabled } from 'tg.ee';
import { useDebouncedCallback } from 'use-debounce';

type TranslationDataEvent =
  | { type: 'translations'; event: TranslationsModifiedData }
  | { type: 'aiReview'; event: AiReviewUpdatedData };

export const useWebsocketService = (
  translationService: ReturnType<typeof useTranslationsService>
) => {
  const [eventBlockers, setEventBlockers] = useState(0);
  const project = useProject();
  const client = useGlobalContext((c) => c.wsClient.client);
  const qaChecksEnabled = useQaChecksEnabled();
  const { available: aiReviewAvailable } = useAiReviewEnabled();

  function updateTranslations(event: TranslationsModifiedData) {
    const translationUpdates = event.data?.translations?.map((translation) => ({
      keyId: translation.relations.key.entityId,
      language: translation.relations.language.data.tag,
      value: {
        ...getModifyingObject(translation.modifications),
        ...(translation.modifications?.text ? { aiReview: undefined } : {}),
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

  function updateQaIssues(events: QaIssuesUpdatedData[]) {
    translationService.changeTranslations(
      events.flatMap((event) =>
        event.data.map((update) => ({
          keyId: update.keyId,
          language: update.languageTag,
          value: {
            id: update.translationId,
            qaIssueCount: update.qaIssueCount,
            qaChecksStale: update.qaChecksStale,
            qaIssues: update.qaIssues,
          },
        }))
      )
    );
  }

  function updateAiReviews(event: AiReviewUpdatedData) {
    translationService.changeTranslations(
      event.data.map((update) => ({
        keyId: update.keyId,
        language: update.languageTag,
        value: {
          id: update.translationId,
          state: update.state,
          aiReview: update.aiReview ?? undefined,
        },
      }))
    );
  }

  const eventQueue = useRef([] as TranslationDataEvent[]);
  const qaEventQueue = useRef([] as QaIssuesUpdatedData[]);

  const handleQueue = () => {
    if (eventBlockers <= 0) {
      eventQueue.current.forEach((e) => {
        if (e.type === 'aiReview') {
          updateAiReviews(e.event);
        } else {
          updateTranslations(e.event);
        }
      });
      eventQueue.current = [];
      if (qaEventQueue.current.length > 0) {
        updateQaIssues(qaEventQueue.current);
        qaEventQueue.current = [];
      }
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
          eventQueue.current.push({ type: 'translations', event });
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
          qaEventQueue.current.push(event);
          handleQueueDelayed();
        }
      );
    }
  }, [project, client, qaChecksEnabled]);

  useEffect(() => {
    if (client && aiReviewAvailable) {
      return client.subscribe(
        `/projects/${project.id}/ai-review-updated`,
        (event) => {
          eventQueue.current.push({ type: 'aiReview', event });
          handleQueueDelayed();
        }
      );
    }
  }, [project, client, aiReviewAvailable]);

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
