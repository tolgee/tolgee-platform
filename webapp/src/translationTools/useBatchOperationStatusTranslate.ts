import { useTranslate } from '@tolgee/react';
import { components } from 'tg.service/apiSchema.generated';

type BatchJobStatus = components['schemas']['BatchJobModel']['status'];

export function useBatchOperationStatusTranslate() {
  const { t } = useTranslate();

  return (status: BatchJobStatus) => {
    switch (status) {
      case 'PENDING':
        return t('batch_operation_status_pending');
      case 'RUNNING':
        return t('batch_operation_status_running');
      case 'SUCCESS':
        return t('batch_operation_status_success');
      case 'FAILED':
        return t('batch_operation_status_failed');
      case 'CANCELLED':
        return t('batch_operation_status_cancelled');
    }
  };
}
