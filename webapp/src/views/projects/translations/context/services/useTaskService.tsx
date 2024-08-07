import { usePutTask, usePutTaskTranslation } from 'tg.service/TranslationHooks';
import { useProject } from 'tg.hooks/useProject';

import { SetTaskTranslationState, UpdateTask } from '../types';
import { useTranslationsService } from './useTranslationsService';
import { confirmation } from 'tg.hooks/confirmation';
import { T } from '@tolgee/react';

type Props = {
  translations: ReturnType<typeof useTranslationsService>;
};

export const useTaskService = ({ translations }: Props) => {
  const project = useProject();
  const putTask = usePutTask();
  const putTaskTranslation = usePutTaskTranslation();

  const updateTask = ({ taskId, data }: UpdateTask) => {
    return putTask.mutateAsync({
      path: { projectId: project.id, taskId: taskId },
      content: { 'application/json': data },
    });
  };

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
        onSuccess(response) {
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
          if (response.taskFinished) {
            confirmation({
              title: <T keyName="task_finished_confirmation_title" />,
              message: <T keyName="task_finished_confirmation_message" />,
              confirmButtonText: (
                <T keyName="task_finished_confirmation_confirm" />
              ),
              onConfirm() {
                updateTask({
                  taskId: data.taskId,
                  data: { state: 'DONE' },
                }).then(() => {
                  translations.refetchTranslations();
                });
              },
            });
          }
        },
      }
    );

  return {
    setTaskTranslationState,
    isLoading: putTaskTranslation.isLoading || putTask.isLoading,
  };
};
