import React, { FC } from 'react';
import { Alert, FormControlLabel, Switch } from '@mui/material';
import { useFormikContext } from 'formik';
import { T, useTranslate } from '@tolgee/react';
import { GenericPlanFormData } from '../cloud/types';

export const PlanArchivedSwitch: FC<{ isUpdate?: boolean }> = ({
  isUpdate,
}) => {
  const { setFieldValue, values } = useFormikContext<GenericPlanFormData>();

  const { t } = useTranslate();

  if (!isUpdate) {
    return null;
  }

  return (
    <>
      <FormControlLabel
        control={
          <Switch
            checked={values.archived ?? false}
            onChange={() => setFieldValue('archived', !values.archived)}
          />
        }
        data-cy="administration-plan-field-archived"
        label={t('administration_cloud_plan_field_archived')}
      />
      {values.archived && (
        <Alert severity="warning" sx={{ mt: 1, mb: 4 }}>
          <T keyName={'admin_billing_plan_archived_warning'} />
        </Alert>
      )}
    </>
  );
};
