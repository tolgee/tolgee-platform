import { useTheme, Typography } from '@mui/material';
import { useTranslate } from '@tolgee/react';

import { components } from 'tg.service/apiSchema.generated';
import { BatchProgress } from './BatchProgress';

type BatchJobModel = components['schemas']['BatchJobModel'];

type Props = {
  data: BatchJobModel;
};

export const BatchIndicator = ({ data }: Props) => {
  const palette = useTheme().palette;

  const { t } = useTranslate();

  const [label, color] = (() => {
    switch (data.status) {
      case 'FAILED':
        return [t('batch_operation_status_failed'), palette.error.main];
      case 'SUCCESS':
        return [t('batch_operation_status_success'), palette.success.main];
      case 'CANCELLED':
        return [t('batch_operation_status_cancelled'), palette.text.secondary];
      case 'PENDING':
        return [t('batch_operation_status_pending'), palette.text.secondary];
      default:
        return [];
    }
  })();

  if (label) {
    return (
      <Typography fontSize={13} fontWeight="bold" color={color}>
        {label}
      </Typography>
    );
  }

  return <BatchProgress max={data.totalItems} progress={data.progress} />;
};
