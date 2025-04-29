import React, { FC } from 'react';
import { Select } from 'tg.component/common/form/fields/Select';
import { MenuItem } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { usePlanFormValues } from '../usePlanFormValues';
import { CloudPlanFormData, MetricType } from '../types';
type CloudPlanMetricTypeSelectFieldProps = {
  parentName?: string;
};

export const CloudPlanMetricTypeSelectField: FC<
  CloudPlanMetricTypeSelectFieldProps
> = ({ parentName }) => {
  const { t } = useTranslate();

  const { values: _values } = usePlanFormValues<CloudPlanFormData>(parentName);

  const options = [
    { value: 'KEYS_SEATS', label: 'Keys & Seats' },
    { value: 'STRINGS', label: 'Strings' },
  ] satisfies { value: MetricType; label: string }[];

  return (
    <Select
      label={t('administration_cloud_plan_field_metric_type')}
      name={`${parentName}metricType`}
      size="small"
      fullWidth
      minHeight={false}
      sx={{ flexBasis: '50%' }}
      data-cy="administration-cloud-plan-field-metric-type"
      renderValue={(val) => options.find((o) => o.value === val)?.label}
    >
      {options.map(({ value, label }) => (
        <MenuItem
          key={value}
          value={value}
          data-cy="administration-cloud-plan-field-metric-type-item"
        >
          {label}
        </MenuItem>
      ))}
    </Select>
  );
};
