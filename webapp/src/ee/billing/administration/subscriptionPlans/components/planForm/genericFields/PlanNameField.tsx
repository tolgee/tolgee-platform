import React, { FC } from 'react';
import { TextField } from 'tg.component/common/form/fields/TextField';
import { useTranslate } from '@tolgee/react';

type PlanNameFieldProps = {
  parentName?: string;
};

export const PlanNameField: FC<PlanNameFieldProps> = ({ parentName = '' }) => {
  const { t } = useTranslate();

  return (
    <TextField
      name={`${parentName}name`}
      size="small"
      label={t('administration_cloud_plan_field_name')}
      fullWidth
      data-cy="administration-plan-field-name"
    />
  );
};
