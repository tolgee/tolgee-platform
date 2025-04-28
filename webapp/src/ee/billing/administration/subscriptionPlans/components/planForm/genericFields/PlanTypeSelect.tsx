import React, { useEffect } from 'react';
import { MenuItem } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { Select } from 'tg.component/common/Select';
import { CloudPlanFormData } from '../cloud/types';

export type PlanTypeOption = {
  value: CloudPlanFormData['type'];
  label: string;
  enabled: boolean;
};

type PlanTypeSelectProps = {
  typeOptions: PlanTypeOption[];
  onChange: (value: CloudPlanFormData['type']) => void;
  value: CloudPlanFormData['type'];
};

export const PlanTypeSelect = ({
  typeOptions,
  onChange,
  value,
}: PlanTypeSelectProps) => {
  const { t } = useTranslate();

  const enabledTypeOptions = typeOptions.filter((t) => t.enabled);

  useEffect(() => {
    const isCurrentValueEnabled = enabledTypeOptions.find(
      (o) => o.value === value
    );
    if (!isCurrentValueEnabled) {
      onChange(enabledTypeOptions[0].value);
    }
  }, [typeOptions, onChange, value]);

  return (
    <Select
      label={t('administration_cloud_plan_field_type')}
      size="small"
      fullWidth
      value={value}
      onChange={(e) => onChange(e.target.value as PlanTypeOption['value'])}
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
