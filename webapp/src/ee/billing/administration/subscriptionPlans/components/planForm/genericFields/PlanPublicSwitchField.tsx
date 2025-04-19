import { Alert, FormControlLabel, Switch, Tooltip } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import React, { FC, ReactElement, ReactNode } from 'react';
import { useFormikContext } from 'formik';
import { GenericPlanFormData } from '../cloud/types';

export const PlanPublicSwitchField: FC<{
  disabled?: boolean;
  disabledInfo?: ReactNode;
}> = ({ disabled, disabledInfo }) => {
  const { t } = useTranslate();

  const { setFieldValue, values } = useFormikContext<GenericPlanFormData>();

  return (
    <Wrapper disabled={disabled} disabledInfo={disabledInfo}>
      <span>
        <FormControlLabel
          disabled={disabled}
          control={
            <Switch
              checked={values.public}
              onChange={() => setFieldValue('public', !values.public)}
            />
          }
          data-cy="administration-plan-field-public"
          label={t('administration_cloud_plan_field_public')}
        />
        {values.public && (
          <Alert severity="warning" sx={{ mt: 1, mb: 4 }}>
            <T keyName={'admin_billing_plan_public_warning'} />
          </Alert>
        )}
      </span>
    </Wrapper>
  );
};

function Wrapper({
  children,
  disabledInfo,
  disabled,
}: {
  children: ReactElement;
  disabled?: boolean;
  disabledInfo?: ReactNode;
}) {
  if (!disabled || !disabledInfo) {
    return children;
  }

  return <Tooltip title={disabledInfo}>{children}</Tooltip>;
}
