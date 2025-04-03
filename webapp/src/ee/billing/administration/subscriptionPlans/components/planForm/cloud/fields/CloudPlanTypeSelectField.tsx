import React, { FC } from 'react';
import { usePlanFormValues } from '../usePlanFormValues';
import { CloudPlanFormData } from '../types';
import {
  PlanTypeOption,
  PlanTypeSelect,
} from '../../genericFields/PlanTypeSelect';

type PlanTypeSelectFieldProps = {
  parentName?: string;
};

export const CloudPlanTypeSelectField: FC<PlanTypeSelectFieldProps> = ({
  parentName,
}) => {
  const { values, setFieldValue } =
    usePlanFormValues<CloudPlanFormData>(parentName);

  const typeOptions = [
    { value: 'PAY_AS_YOU_GO', label: 'Pay as you go', enabled: !values.free },
    { value: 'FIXED', label: 'Fixed', enabled: true },
  ] as PlanTypeOption[];

  return (
    <PlanTypeSelect
      typeOptions={typeOptions}
      onChange={(value) => setFieldValue(`type`, value)}
      value={values['type']}
    />
  );
};
