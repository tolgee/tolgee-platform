import { FC } from 'react';
import { TextField } from 'tg.component/common/form/fields/TextField';
import { Box, Typography } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { PlanType } from '../../../../../component/Plan/types';

type PlanIncludedUsageFieldsProps = {
  parentName?: string;
  metricType?: PlanType['metricType'];
};

export const PlanIncludedUsageFields: FC<PlanIncludedUsageFieldsProps> = ({
  parentName = '',
  metricType = 'KEYS_SEATS',
}) => {
  const { t } = useTranslate();

  return (
    <>
      <Typography sx={{ mt: 2 }}>
        {t('administration_cloud_plan_form_limits_title')}
      </Typography>
      <Box
        display="grid"
        gridTemplateColumns="repeat(3, 1fr)"
        gap={2}
        sx={{ mt: 1 }}
      >
        <TextField
          name={`${parentName}includedUsage.mtCredits`}
          size="small"
          type="number"
          fullWidth
          data-cy="administration-cloud-plan-field-included-mt-credits"
          label={t('administration_cloud_plan_field_included_mt_credits')}
        />

        {metricType == 'STRINGS' && (
          <TextField
            name={`${parentName}includedUsage.translations`}
            size="small"
            type="number"
            fullWidth
            data-cy="administration-cloud-plan-field-included-translations"
            label={t('administration_cloud_plan_field_included_translations')}
          />
        )}

        {metricType == 'KEYS_SEATS' && (
          <>
            <TextField
              name={`${parentName}includedUsage.keys`}
              size="small"
              type="number"
              fullWidth
              data-cy="administration-cloud-plan-field-included-keys"
              label={t('administration_cloud_plan_field_included_keys')}
            />
            <TextField
              name={`${parentName}includedUsage.seats`}
              size="small"
              type="number"
              fullWidth
              data-cy="administration-cloud-plan-field-included-seats"
              label={t('administration_cloud_plan_field_included_seats')}
            />
          </>
        )}
      </Box>
    </>
  );
};
