import { TextField } from 'tg.component/common/form/fields/TextField';
import { useTranslate } from '@tolgee/react';
import { Box, FormControlLabel, Switch } from '@mui/material';
import { useFormikContext } from 'formik';
import React, { FC } from 'react';
import { CloudPlanPricesAndLimits } from './CloudPlanPricesAndLimits';
import { PlanNonCommercialSwitch } from './PlanNonCommercialSwitch';
import { useCloudPlanFormValues } from '../useCloudPlanFormValues';
import { CloudPlanTypeSelectField } from './CloudPlanTypeSelectField';
import { PlanStripeProductSelectField } from './PlanStripeProductSelectField';
import { CloudPlanMetricTypeSelectField } from './CloudPlanMetricTypeSelectField';
import { PlanEnabledFeaturesField } from './PlanEnabledFeaturesField';

export const CloudPlanFields: FC<{
  parentName?: string;
  isUpdate: boolean;
  canEditPrices: boolean;
}> = ({ parentName, isUpdate, canEditPrices }) => {
  const { t } = useTranslate();

  const { setFieldValue, errors } = useFormikContext<any>();

  const { values } = useCloudPlanFormValues(parentName);

  parentName = parentName ? parentName + '.' : '';

  function onFreeChange() {
    setFieldValue(`${parentName}free`, !values.free);
  }

  return (
    <>
      <TextField
        name={`${parentName}name`}
        size="small"
        label={t('administration_cloud_plan_field_name')}
        fullWidth
        data-cy="administration-cloud-plan-field-name"
      />
      <FormControlLabel
        disabled={isUpdate}
        control={
          <Switch checked={values.free} onChange={() => onFreeChange()} />
        }
        data-cy="administration-cloud-plan-field-free"
        label={t('administration_cloud_plan_field_free')}
      />
      <Box
        sx={{
          display: 'grid',
          gap: 2,
          mt: 2,
          gridTemplateColumns: '1fr 1fr 1fr',
        }}
      >
        <CloudPlanTypeSelectField parentName={parentName} />
        <CloudPlanMetricTypeSelectField parentName={parentName} />
        <PlanStripeProductSelectField parentName={parentName} />
      </Box>

      <CloudPlanPricesAndLimits
        parentName={parentName}
        canEditPrices={canEditPrices}
      />

      <PlanEnabledFeaturesField parentName={parentName} />
      <PlanNonCommercialSwitch />
    </>
  );
};
