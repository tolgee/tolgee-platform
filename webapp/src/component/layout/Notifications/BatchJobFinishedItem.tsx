import { default as React, FunctionComponent } from 'react';
import { Box } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import {
  NotificationItem,
  NotificationItemProps,
} from 'tg.component/layout/Notifications/NotificationItem';
import { useBatchOperationTypeTranslate } from 'tg.translationTools/useBatchOperationTypeTranslation';
import { LINKS, PARAMS } from 'tg.constants/links';

type Props = NotificationItemProps;

export const BatchJobFinishedItem: FunctionComponent<Props> = ({
  notification,
  ...props
}) => {
  const { t } = useTranslate();
  const translateType = useBatchOperationTypeTranslate();

  const batchJob = notification.linkedBatchJob;
  const succeeded = batchJob?.status === 'SUCCESS';

  const destinationUrl = notification.project
    ? LINKS.PROJECT_TRANSLATIONS.build({
        [PARAMS.PROJECT_ID]: notification.project.id,
      })
    : undefined;

  return (
    <NotificationItem
      notification={notification}
      destinationUrl={destinationUrl}
      {...props}
    >
      <Box>
        <b>{batchJob ? translateType(batchJob.type) : ''}</b>
        {'\u205F'}
        {succeeded
          ? t('notification_batch_job_completed')
          : t('notification_batch_job_failed')}
      </Box>
      {batchJob && (
        <Box>
          <T
            keyName="batch_operation_progress"
            params={{
              progress: batchJob.progress,
              totalItems: batchJob.totalItems,
            }}
          />
        </Box>
      )}
    </NotificationItem>
  );
};
