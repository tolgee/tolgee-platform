import * as React from 'react';
import { FC } from 'react';
import { Box, Typography } from '@mui/material';
import { TextField } from 'tg.component/common/form/fields/TextField';
import { useTranslate } from '@tolgee/react';
import { PlanType } from '../../../../../component/Plan/types';
import { usePlanFormValues } from '../cloud/usePlanFormValues';
import { CloudPlanFormData, SelfHostedEePlanFormData } from '../cloud/types';
import { useSetZeroPricesWhenFree } from '../../../../subscriptions/components/generic/useSetZeroPricesWhenFree';

export interface PlanPricesFieldsProps {
  parentName: string;
  metricType: PlanType['metricType'];
  isPayAsYouGo: boolean;
}

export const PlanPricesFields: FC<PlanPricesFieldsProps> = ({
  parentName,
  metricType,
  isPayAsYouGo,
}) => {
  const { values } = usePlanFormValues<
    CloudPlanFormData | SelfHostedEePlanFormData
  >(parentName);

  useSetZeroPricesWhenFree({ parentName });

  const { t } = useTranslate();

  if (values.free) {
    return null;
  }

  return (
    <>
      <Typography sx={{ mt: 4 }} variant="h3">
        {t('administration_cloud_plan_form_prices_title')}
      </Typography>
      <Typography>
        {t('administration_cloud_plan_form_prices_per_subscription')}
      </Typography>
      <Box
        display="grid"
        gridTemplateColumns="repeat(2, 1fr)"
        gap={2}
        sx={{ mt: 1 }}
      >
        <TextField
          name={`${parentName}prices.subscriptionMonthly`}
          size="small"
          data-cy="administration-plan-field-price-monthly"
          label={t('administration_cloud_plan_field_price_monthly')}
          type="number"
          fullWidth
        />
        <TextField
          name={`${parentName}prices.subscriptionYearly`}
          size="small"
          data-cy="administration-plan-field-price-yearly"
          label={t('administration_cloud_plan_field_price_yearly')}
          type="number"
          fullWidth
        />
      </Box>

      <Typography>
        {t('administration_cloud_plan_form_prices_per_usage')}
      </Typography>

      <Box
        display="grid"
        gridTemplateColumns="repeat(3, 1fr)"
        gap={2}
        sx={{ mt: 1 }}
      >
        <TextField
          name={`${parentName}prices.perThousandMtCredits`}
          size="small"
          data-cy="administration-cloud-plan-field-price-per-thousand-mt-credits"
          label={t(
            'administration_cloud_plan_field_price_per_thousand_mt_credits'
          )}
          type="number"
          fullWidth
          disabled={!isPayAsYouGo}
        />
        {metricType == 'STRINGS' && (
          <>
            <TextField
              name={`${parentName}prices.perThousandTranslations`}
              size="small"
              data-cy="administration-cloud-plan-field-price-per-thousand-translations"
              label={t(
                'administration_cloud_plan_field_price_per_thousand_translations'
              )}
              type="number"
              fullWidth
              disabled={!isPayAsYouGo}
            />
          </>
        )}
        {metricType == 'KEYS_SEATS' && (
          <>
            <TextField
              name={`${parentName}prices.perThousandKeys`}
              size="small"
              data-cy="administration-cloud-plan-field-price-per-thousand-keys"
              label={t(
                'administration_cloud_plan_field_price_per_thousand_keys'
              )}
              type="number"
              fullWidth
              disabled={!isPayAsYouGo}
            />
            <TextField
              name={`${parentName}prices.perSeat`}
              size="small"
              data-cy="administration-plan-field-price-per-seat"
              label={t('administration_cloud_plan_field_price_per_seat')}
              type="number"
              fullWidth
              disabled={!isPayAsYouGo}
            />
          </>
        )}
      </Box>
    </>
  );
};
