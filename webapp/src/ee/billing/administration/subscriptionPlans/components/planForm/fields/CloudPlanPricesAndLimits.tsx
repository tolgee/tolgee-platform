import { FC } from 'react';
import { useTranslate } from '@tolgee/react';
import { Box, Tooltip, Typography } from '@mui/material';
import { TextField } from 'tg.component/common/form/fields/TextField';
import { CloudPlanFormData } from '../CloudPlanFormBase';

export const CloudPlanPricesAndLimits: FC<{
  parentName?: string;
  values: CloudPlanFormData;
  canEditPrices: boolean;
}> = ({ values, parentName, canEditPrices }) => {
  const { t } = useTranslate();

  const Wrapper = ({ children }) => {
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

  return (
    <Wrapper>
      {!values.free && (
        <>
          <Typography sx={{ mt: 2 }}>
            {t('administration_cloud_plan_form_prices_title')}
          </Typography>
          <Box
            display="grid"
            gridTemplateColumns="repeat(4, 1fr)"
            gap={2}
            sx={{ mt: 1 }}
          >
            <TextField
              name={`${parentName}prices.subscriptionMonthly`}
              size="small"
              data-cy="administration-cloud-plan-field-price-monthly"
              label={t('administration_cloud_plan_field_price_monthly')}
              type="number"
              fullWidth
            />
            <TextField
              name={`${parentName}prices.subscriptionYearly`}
              size="small"
              data-cy="administration-cloud-plan-field-price-yearly"
              label={t('administration_cloud_plan_field_price_yearly')}
              type="number"
              fullWidth
            />
            <TextField
              name={`${parentName}prices.perThousandTranslations`}
              size="small"
              data-cy="administration-cloud-plan-field-price-per-thousand-translations"
              label={t(
                'administration_cloud_plan_field_price_per_thousand_translations'
              )}
              type="number"
              fullWidth
              disabled={values.type !== 'PAY_AS_YOU_GO'}
            />
            <TextField
              name={`${parentName}prices.perThousandMtCredits`}
              size="small"
              data-cy="administration-cloud-plan-field-price-per-thousand-mt-credits"
              label={t(
                'administration_cloud_plan_field_price_per_thousand_mt_credits'
              )}
              type="number"
              fullWidth
              disabled={values.type !== 'PAY_AS_YOU_GO'}
            />
          </Box>
        </>
      )}
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
