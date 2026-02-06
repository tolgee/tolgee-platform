import {
  Box,
  FormControlLabel,
  Radio,
  RadioGroup,
  Typography,
} from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { LoadingSkeleton } from 'tg.component/LoadingSkeleton';
import { components } from 'tg.service/apiSchema.generated';

type BatchTranslateInfoResponse =
  components['schemas']['BatchTranslateInfoResponse'];

type Props = {
  batchInfo: BatchTranslateInfoResponse | undefined;
  isLoading: boolean;
  value: 'instant' | 'batch';
  onChange: (mode: 'instant' | 'batch') => void;
};

export const BatchModeSelector = ({
  batchInfo,
  isLoading,
  value,
  onChange,
}: Props) => {
  const { t } = useTranslate();

  if (isLoading) {
    return <LoadingSkeleton sx={{ width: 200, height: 32 }} />;
  }

  return (
    <Box>
      <Typography variant="caption" color="textSecondary">
        {t('batch_mode_selector_label')}
      </Typography>
      <RadioGroup
        row
        value={value}
        onChange={(e) => onChange(e.target.value as 'instant' | 'batch')}
      >
        <FormControlLabel
          value="instant"
          control={<Radio size="small" />}
          label={t('batch_mode_instant')}
          data-cy="batch-mode-instant"
        />
        <FormControlLabel
          value="batch"
          control={<Radio size="small" />}
          label={t('batch_mode_batch', {
            discount: batchInfo?.discountPercent ?? 50,
          })}
          data-cy="batch-mode-batch"
        />
      </RadioGroup>
    </Box>
  );
};
