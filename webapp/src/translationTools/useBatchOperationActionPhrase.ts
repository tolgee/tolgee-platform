import { useTranslate } from '@tolgee/react';
import { components } from 'tg.service/apiSchema.generated';

type BatchJobType = components['schemas']['BatchJobModel']['type'];

export function useBatchOperationActionPhrase() {
  const { t } = useTranslate();

  return (type: BatchJobType) => {
    switch (type) {
      case 'AI_PLAYGROUND_TRANSLATE':
      case 'MACHINE_TRANSLATE':
      case 'AUTO_TRANSLATE':
        return t('batch_operation_action_translating');
      case 'PRE_TRANSLATE_BT_TM':
        return t('batch_operation_action_finding_translations');
      case 'DELETE_KEYS':
        return t('batch_operation_action_deleting_keys');
      case 'CLEAR_TRANSLATIONS':
        return t('batch_operation_action_clearing');
      case 'COPY_TRANSLATIONS':
        return t('batch_operation_action_copying');
      case 'TAG_KEYS':
      case 'UNTAG_KEYS':
        return t('batch_operation_action_updating_tags');
      case 'SET_TRANSLATIONS_STATE':
        return t('batch_operation_action_updating_state');
      case 'SET_KEYS_NAMESPACE':
        return t('batch_operation_action_updating_namespace');
      case 'ASSIGN_TRANSLATION_LABEL':
      case 'UNASSIGN_TRANSLATION_LABEL':
        return t('batch_operation_action_updating_labels');
      default:
        return t('batch_operation_action_processing');
    }
  };
}
