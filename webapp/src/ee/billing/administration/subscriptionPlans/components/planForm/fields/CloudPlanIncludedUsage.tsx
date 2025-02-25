import { FC } from 'react';
import { TextField } from 'tg.component/common/form/fields/TextField';
import { Box, Typography } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { useCloudPlanFormValues } from '../useCloudPlanFormValues';

type CloudPlanPricesProps = {
  parentName: string | undefined;
};

export const CloudPlanIncludedUsage: FC<CloudPlanPricesProps> = ({
  parentName,
}) => {
  const { t } = useTranslate();

  const { values } = useCloudPlanFormValues(parentName);

  return (
    <>
      <Typography sx={{ mt: 2 }}>
        {t('administration_cloud_plan_form_limits_title')}
      </Typography>
      <Box
        display="grid"
        gridTemplateColumns="repeat(4, 1fr)"
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

        {values.metricType == 'STRINGS' && (
          <TextField
            name={`${parentName}includedUsage.translations`}
            size="small"
            type="number"
            fullWidth
            data-cy="administration-cloud-plan-field-included-translations"
            label={
              values.type === 'SLOTS_FIXED'
                ? t(
                    'administration_cloud_plan_field_included_translation_slots'
                  )
                : t('administration_cloud_plan_field_included_translations')
            }
          />
        )}

        {values.metricType == 'SEATS_KEYS' && (
          <>
            {/*TODO: Test that for fixed plan we cannot set the prices in the backend tests  */}
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
              data-cy="administration-cloud-plan-field-included-keys"
              label={t('administration_cloud_plan_field_included_seats')}
            />
          </>
        )}
      </Box>
    </>
  );
};
