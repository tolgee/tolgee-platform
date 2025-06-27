import React from 'react';
import { FormControlLabel, Switch } from '@mui/material';
import { useFormikContext } from 'formik';
import { usePlanFormValues } from '../cloud/usePlanFormValues';
import { useTranslate } from '@tolgee/react';

type Props = {
  parentName?: string;
  isUpdate?: boolean;
};

export const PlanNewStripeProductSwitch: React.FC<Props> = ({
  parentName = '',
  isUpdate,
}) => {
  const { setFieldValue } = useFormikContext<any>();

  const { values } = usePlanFormValues(parentName);

  const { t } = useTranslate();

  function onChange() {
    setFieldValue(`${parentName}newStripeProduct`, !values.newStripeProduct);
  }

  return (
    <FormControlLabel
      disabled={isUpdate}
      control={<Switch checked={values.newStripeProduct} onChange={onChange} />}
      data-cy="administration-cloud-plan-field-select-existing-stripe-product"
      label={t('administration_cloud_plan_field_new_stripe_product')}
    />
  );
};
