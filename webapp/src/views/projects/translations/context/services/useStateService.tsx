import { usePutTranslationState } from 'tg.service/TranslationHooks';
import { useProject } from 'tg.hooks/useProject';

import { SetTranslationState } from '../types';
import { useTranslationsService } from './useTranslationsService';
import { useTaskService } from './useTaskService';

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
          const translation = key?.translations[data.language];
          const task = translation?.tasks?.[0];
          if (
            data.state === 'REVIEWED' &&
            task?.userAssigned &&
            task.type === 'REVIEW' &&
            !task.done
          ) {
            taskService.setTaskTranslationState({
              keyId: data.keyId,
              taskId: task.id,
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
