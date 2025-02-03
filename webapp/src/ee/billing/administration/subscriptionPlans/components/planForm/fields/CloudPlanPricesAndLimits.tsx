import { FC } from 'react';
import { useTranslate } from '@tolgee/react';
import { Box, Tooltip, Typography } from '@mui/material';
import { TextField } from 'tg.component/common/form/fields/TextField';
import { CloudPlanFormData } from '../CloudPlanFormBase';
import { CloudPlanPrices } from './CloudPlanPrices';

export const CloudPlanPricesAndLimits: FC<{
  parentName?: string;
  values: CloudPlanFormData;
  canEditPrices: boolean;
}> = ({ values, parentName, canEditPrices }) => {
  const { t } = useTranslate();

  return (
    <Wrapper canEditPrices={canEditPrices}>
      <CloudPlanPrices parentName={parentName} />
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
        <TextField
          name={`${parentName}includedUsage.translations`}
          size="small"
          type="number"
          fullWidth
          data-cy="administration-cloud-plan-field-included-translations"
          label={
            values.type === 'SLOTS_FIXED'
              ? t('administration_cloud_plan_field_included_translation_slots')
              : t('administration_cloud_plan_field_included_translations')
          }
        />
      </Box>
    </Wrapper>
  );
};

const Wrapper = ({ children, canEditPrices }) => {
  const { t } = useTranslate();

  if (!canEditPrices) {
    return (
      <Tooltip title={t('admin-billing-cannot-edit-prices-tooltip')}>
        <span>
          <Box sx={{ pointerEvents: 'none', opacity: 0.5 }}>{children}</Box>
        </span>
      </Tooltip>
    );
  }

  return <>{children}</>;
};
