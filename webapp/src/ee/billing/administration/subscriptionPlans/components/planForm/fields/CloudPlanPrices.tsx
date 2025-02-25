import { FC, useEffect, useState } from 'react';
import { TextField } from 'tg.component/common/form/fields/TextField';
import { Box, Typography } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { CloudPlanFormData } from '../CloudPlanFormBase';
import { useFormikContext } from 'formik';
import { getCloudPlanInitialValues } from '../getCloudPlanInitialValues';
import { useCloudPlanFormValues } from '../useCloudPlanFormValues';

type CloudPlanPricesProps = {
  parentName: string | undefined;
};

export const CloudPlanPrices: FC<CloudPlanPricesProps> = ({ parentName }) => {
  const { t } = useTranslate();

  // Here we store the non-zero prices to be able to restore them when the plan is set back to non-free
  const [nonZeroPrices, setNonZeroPrices] =
    useState<CloudPlanFormData['prices']>();

  const { values, setFieldValue } = useCloudPlanFormValues(parentName);

  const free = values.free;
  const prices = values.prices;
  const type = values.type;
  const metricType = values.metricType;

  function setPriceValuesZero() {
    setNonZeroPrices(prices);
    const zeroPrices = Object.entries(prices).reduce(
      (acc, [key]) => ({ ...acc, [key]: 0 }),
      {}
    );

    setFieldValue(`prices`, zeroPrices);
  }

  function setPriceValuesNonZero() {
    setFieldValue(`prices`, nonZeroPrices);
  }

  useEffect(() => {
    if (free) {
      setPriceValuesZero();
      return;
    }
    if (nonZeroPrices) {
      setPriceValuesNonZero();
    }
  }, [free]);

  if (free) {
    return null;
  }

  return (
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
          name={`${parentName}prices.perThousandMtCredits`}
          size="small"
          data-cy="administration-cloud-plan-field-price-per-thousand-mt-credits"
          label={t(
            'administration_cloud_plan_field_price_per_thousand_mt_credits'
          )}
          type="number"
          fullWidth
          disabled={type !== 'PAY_AS_YOU_GO'}
        />
        {/*TODO: Split usage based and subscription based prices*/}
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
              disabled={type !== 'PAY_AS_YOU_GO'}
            />
          </>
        )}
        {metricType == 'SEATS_KEYS' && (
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
              disabled={type !== 'PAY_AS_YOU_GO'}
            />
            <TextField
              name={`${parentName}prices.perSeat`}
              size="small"
              data-cy="administration-cloud-plan-field-price-per-seat"
              label={t('administration_cloud_plan_field_price_per_seat')}
              type="number"
              fullWidth
              disabled={type !== 'PAY_AS_YOU_GO'}
            />
          </>
        )}
      </Box>
    </>
  );
};
