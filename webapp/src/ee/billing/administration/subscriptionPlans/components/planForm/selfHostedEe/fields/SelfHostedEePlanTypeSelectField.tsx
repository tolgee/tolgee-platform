import React, { FC } from 'react';
import { SelfHostedEePlanFormData } from '../../cloud/types';
import { usePlanFormValues } from '../../cloud/usePlanFormValues';
import {
  PlanTypeOption,
  PlanTypeSelect,
} from '../../genericFields/PlanTypeSelect';

type PlanTypeSelectFieldProps = {
  parentName?: string;
};

export const SelfHostedEePlanTypeSelectField: FC<PlanTypeSelectFieldProps> = ({
  parentName = '',
}) => {
  const { values, setFieldValue } =
    usePlanFormValues<SelfHostedEePlanFormData>(parentName);

  const typeOptions = [
    { value: 'PAY_AS_YOU_GO', label: 'Pay as you go', enabled: !values.free },
    { value: 'FIXED', label: 'Fixed', enabled: true },
  ] as PlanTypeOption[];

  function onChange(value: PlanTypeOption['value']) {
    switch (value) {
      case 'PAY_AS_YOU_GO':
        setFieldValue('isPayAsYouGo', true);
        break;
      case 'FIXED':
        setFieldValue('isPayAsYouGo', false);
        break;
    }
  }

  function getValue() {
    if (values['isPayAsYouGo']) {
      return 'PAY_AS_YOU_GO';
    }

    return 'FIXED';
  }

  return (
    <PlanTypeSelect
      typeOptions={typeOptions}
      onChange={onChange}
      value={getValue()}
    />
  );
};
