import React, { FC, useEffect } from 'react';
import { Select } from 'tg.component/common/form/fields/Select';
import { MenuItem } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { useCloudPlanFormValues } from '../useCloudPlanFormValues';

type PlanTypeSelectFieldProps = {
  parentName?: string;
};

export const CloudPlanTypeSelectField: FC<PlanTypeSelectFieldProps> = ({
  parentName,
}) => {
  const { t } = useTranslate();

  const { values, setFieldValue } = useCloudPlanFormValues(parentName);

  const typeOptions = [
    { value: 'PAY_AS_YOU_GO', label: 'Pay as you go', enabled: !values.free },
    { value: 'FIXED', label: 'Fixed', enabled: true },
    { value: 'SLOTS_FIXED', label: 'Slots fixed (legacy)', enabled: true },
  ];

  const enabledTypeOptions = typeOptions.filter((t) => t.enabled);

  useEffect(() => {
    if (!enabledTypeOptions.find((o) => o.value === values.type)) {
      setFieldValue(`type`, enabledTypeOptions[0].value);
    }
  }, [values.free]);

  return (
    <Select
      label={t('administration_cloud_plan_field_type')}
      name={`${parentName}type`}
      size="small"
      fullWidth
      minHeight={false}
      sx={{ flexBasis: '50%' }}
      data-cy="administration-cloud-plan-field-type"
      renderValue={(val) =>
        enabledTypeOptions.find((o) => o.value === val)?.label
      }
    >
      {enabledTypeOptions.map(({ value, label }) => (
        <MenuItem
          key={value}
          value={value}
          data-cy="administration-cloud-plan-field-type-item"
        >
          {label}
        </MenuItem>
      ))}
    </Select>
  );
};
