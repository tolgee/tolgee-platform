import React, { FC } from 'react';
import { FormControlLabel, Switch } from '@mui/material';
import { useFormikContext } from 'formik';
import { usePlanFormValues } from '../cloud/usePlanFormValues';
import { useTranslate } from '@tolgee/react';

type PlanFreePlanSwitchProps = {
  parentName?: string;
  isUpdate?: boolean;
};

export const PlanFreePlanSwitch: FC<PlanFreePlanSwitchProps> = ({
  parentName = '',
  isUpdate,
}) => {
  const { setFieldValue } = useFormikContext<any>();

  const { values } = usePlanFormValues(parentName);

  const { t } = useTranslate();

  function onFreeChange() {
    setFieldValue(`${parentName}free`, !values.free);
  }

  return (
    <FormControlLabel
      disabled={isUpdate}
      control={<Switch checked={values.free} onChange={() => onFreeChange()} />}
      data-cy="administration-cloud-plan-field-free"
      label={t('administration_cloud_plan_field_free')}
    />
  );
};
