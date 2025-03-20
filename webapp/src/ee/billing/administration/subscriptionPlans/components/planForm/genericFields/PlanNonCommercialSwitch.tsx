import React, { FC } from 'react';
import { FormControlLabel, Switch } from '@mui/material';
import { useFormikContext } from 'formik';
import { useTranslate } from '@tolgee/react';
import { CloudPlanFormData } from '../cloud/types';

export const PlanNonCommercialSwitch: FC = () => {
  const { setFieldValue, values } = useFormikContext<CloudPlanFormData>();

  const { t } = useTranslate();

  return (
    <>
      <FormControlLabel
        control={
          <Switch
            checked={values.nonCommercial}
            onChange={() =>
              setFieldValue('nonCommercial', !values.nonCommercial)
            }
          />
        }
        data-cy="administration-plan-field-non-commercial"
        label={t('administration_cloud_plan_field_non_commercial')}
      />
    </>
  );
};
