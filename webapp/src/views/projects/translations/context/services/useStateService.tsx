import { usePutTranslationState } from 'tg.service/TranslationHooks';
import { useProject } from 'tg.hooks/useProject';

import { SetTranslationState } from '../types';
import { useTranslationsService } from './useTranslationsService';
import { useTaskService } from './useTaskService';
import { taskReviewControlsShouldBeVisible } from './utils';

type Props = {
  translations: ReturnType<typeof useTranslationsService>;
  taskService: ReturnType<typeof useTaskService>;
};

export const useStateService = ({ translations, taskService }: Props) => {
  const putTranslationState = usePutTranslationState();
  const project = useProject();

  const changeState = (data: SetTranslationState) =>
    putTranslationState.mutateAsync(
      {
        path: {
          projectId: project.id,
          translationId: data.translationId,
          state: data.state,
        },
      },
      {
        onSuccess(response) {
          translations.changeTranslations([
            { keyId: data.keyId, language: data.language, value: response },
          ]);
          const key = translations.fixedTranslations?.find(
            (k) => k.keyId === data.keyId
          );
          const firstTask = key?.tasks?.find(
            (t) => t.languageTag === data.language
          );
          if (
            data.state === 'REVIEWED' &&
            firstTask &&
            taskReviewControlsShouldBeVisible(firstTask)
          ) {
            taskService.setTaskTranslationState({
              keyId: data.keyId,
              taskNumber: firstTask.number,
              done: true,
            });
          }
        },
      }
    );

  return {
    changeState,
    isLoading: putTranslationState.isLoading,
  };
};
