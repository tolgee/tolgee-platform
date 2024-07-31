import { usePutTaskTranslation } from 'tg.service/TranslationHooks';
import { useProject } from 'tg.hooks/useProject';

import { SetTaskTranslationState } from '../types';
import { useTranslationsService } from './useTranslationsService';

type Props = {
  translations: ReturnType<typeof useTranslationsService>;
};

export const useTaskService = ({ translations }: Props) => {
  const putTaskTranslation = usePutTaskTranslation();
  const project = useProject();

  const setTaskTranslationState = (data: SetTaskTranslationState) =>
    putTaskTranslation.mutateAsync(
      {
        path: {
          projectId: project.id,
          taskId: data.taskId,
          keyId: data.keyId,
        },
        content: {
          'application/json': {
            done: data.done,
          },
        },
      },
      {
        onSuccess() {
          const key = translations.fixedTranslations?.find(
            (t) => t.keyId === data.keyId
          );
          const translation = Object.entries(key?.translations ?? {}).find(
            ([_, t]) => t.tasks?.find((tk) => tk.id === data.taskId)
          );
          if (translation) {
            translations.updateTranslation({
              keyId: data.keyId,
              lang: translation[0],
              data: {
                tasks: translation[1].tasks?.map((t) => ({
                  ...t,
                  done: data.done,
                })),
              },
            });
          }
        },
      }
    );

  return {
    setTaskTranslationState,
    isLoading: putTaskTranslation.isLoading,
  };
};
