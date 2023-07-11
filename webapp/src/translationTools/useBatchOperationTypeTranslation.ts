import { useTranslate } from '@tolgee/react';
import { components } from 'tg.service/apiSchema.generated';

type BatchJobType = components['schemas']['BatchJobModel']['type'];

export function useBatchOperationTypeTranslate() {
  const { t } = useTranslate();

  return (type: BatchJobType) => {
    switch (type) {
      case 'DELETE_KEYS':
        return t('batch_operation_type_delete_keys');
      case 'AUTO_TRANSLATION':
        return t('batch_operation_type_translation');
      case 'COPY_TRANSLATIONS':
        return t('batch_operation_type_copy_translations');
      case 'CLEAR_TRANSLATIONS':
        return t('batch_operation_type_clear_translations');
      case 'SET_TRANSLATIONS_STATE':
        return t('batch_operation_set_translations_state');
      default:
        return type;
    }
  };
}
