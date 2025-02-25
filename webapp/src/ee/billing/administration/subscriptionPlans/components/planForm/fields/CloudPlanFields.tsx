import { TextField } from 'tg.component/common/form/fields/TextField';
import { useTranslate } from '@tolgee/react';
import {
  Box,
  Checkbox,
  FormControlLabel,
  Switch,
  Typography,
} from '@mui/material';
import { Field, FieldProps, useFormikContext } from 'formik';
import { useBillingApiQuery } from 'tg.service/http/useQueryApi';
import React, { FC } from 'react';
import { CloudPlanPricesAndLimits } from './CloudPlanPricesAndLimits';
import { PlanNonCommercialSwitch } from './PlanNonCommercialSwitch';
import { useCloudPlanFormValues } from '../useCloudPlanFormValues';
import { CloudPlanTypeSelectField } from './CloudPlanTypeSelectField';
import { StripeProductSelectField } from './StripeProductSelectField';
import { CloudPlanMetricTypeSelectField } from './CloudPlanMetricTypeSelectField';
import { PlanEnabledFeaturesField } from './PlanEnabledFeaturesField';

export const CloudPlanFields: FC<{
  parentName?: string;
  isUpdate: boolean;
  canEditPrices: boolean;
}> = ({ parentName, isUpdate, canEditPrices }) => {
  const { t } = useTranslate();

  const { setFieldValue } = useFormikContext<any>();

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
        <StripeProductSelectField />
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
