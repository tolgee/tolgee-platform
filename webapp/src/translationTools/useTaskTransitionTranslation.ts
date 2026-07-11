import { useTranslate } from '@tolgee/react';
import { components } from 'tg.service/apiSchema.generated';

type TaskType = components['schemas']['TaskModel']['type'];

export function useTaskTransitionTranslation() {
  const { t } = useTranslate();

  return (type: TaskType, done: boolean) => {
    switch (type) {
      case 'REVIEW':
        if (done) {
          return t('task_transition_done_to_review');
        } else {
          return t('task_transition_to_review_done');
        }
      case 'TRANSLATE':
        if (done) {
          return t('task_transition_done_to_translate');
        } else {
          return t('task_transition_to_translate_done');
        }
    }
  };
}
