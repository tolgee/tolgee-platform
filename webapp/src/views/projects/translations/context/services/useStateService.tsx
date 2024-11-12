import { usePutTranslationState } from 'tg.service/TranslationHooks';
import { useProject } from 'tg.hooks/useProject';

import { SetTranslationState } from '../types';
import { useTranslationsService } from './useTranslationsService';
import { useTaskService } from './useTaskService';
import { PrefilterType } from '../../prefilters/usePrefilter';

type Props = {
  translations: ReturnType<typeof useTranslationsService>;
  taskService: ReturnType<typeof useTaskService>;
  prefilter: PrefilterType | undefined;
};

export const useStateService = ({
  translations,
  taskService,
  prefilter,
}: Props) => {
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
          const task = key?.tasks?.find((t) => t.languageTag === data.language);
          if (
            data.state === 'REVIEWED' &&
            task?.userAssigned &&
            prefilter?.task === task?.number &&
            task.type === 'REVIEW' &&
            !task.done
          ) {
            taskService.setTaskTranslationState({
              keyId: data.keyId,
              taskNumber: task.number,
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
