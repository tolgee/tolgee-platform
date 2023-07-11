import { Typography, Box } from '@mui/material';

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
  const statusColor = useStatusColor()(data.status);
  const statusLabel = useBatchOperationStatusTranslate()(data.status);

  const isStatic = STATIC_STATUSES.includes(data.status);

  if (isStatic) {
    return (
      <Typography fontSize={13} fontWeight="bold" color={statusColor}>
        {statusLabel}
      </Typography>
    );
  }

  return (
    <Box width={width}>
      <BatchProgress max={data.totalItems} progress={data.progress} />
    </Box>
  );
};
