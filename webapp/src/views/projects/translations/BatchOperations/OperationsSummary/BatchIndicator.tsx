import { Typography, Box } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { Clock } from '@untitled-ui/icons-react';

import { components } from 'tg.service/apiSchema.generated';
import { useBatchOperationStatusTranslate } from 'tg.translationTools/useBatchOperationStatusTranslate';
import { BatchProgress } from './BatchProgress';
import { STATIC_STATUSES, useStatusColor } from './utils';

type BatchJobModel = components['schemas']['BatchJobModel'];

type Props = {
  data: BatchJobModel;
  width?: string;
};

export const BatchIndicator = ({ data, width = '100px' }: Props) => {
  const { t } = useTranslate();
  const statusColor = useStatusColor()(data.status);
  const statusLabel = useBatchOperationStatusTranslate()(data.status);

  const isStatic = STATIC_STATUSES.includes(data.status);

  const isBatchProcessing =
    data.batchApiPhase === 'WAITING_FOR_OPENAI' || data.batchApiPhase === 'SUBMITTING';

  if (isStatic) {
    return (
      <Typography fontSize={13} fontWeight="bold" color={statusColor}>
        {statusLabel}
      </Typography>
    );
  }

  if (isBatchProcessing) {
    return (
      <Box display="flex" alignItems="center" gap={0.5}>
        <Clock width={14} height={14} />
        <Typography fontSize={13} color="textSecondary">
          {t('batch_indicator_batch_processing')}
        </Typography>
      </Box>
    );
  }

  return (
    <Box width={width}>
      <BatchProgress max={data.totalItems} progress={data.progress} />
    </Box>
  );
};
