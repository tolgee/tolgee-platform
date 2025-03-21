import {
  SearchSelect,
  SelectItem,
} from 'tg.component/searchSelect/SearchSelect';
import React from 'react';

type GenericPlanType = { id: number; name: string };

export interface GenericPlanSelector<T extends GenericPlanType> {
  organizationId?: number;
  onPlanChange?: (planId: T) => void;
  value?: number;
  onChange?: (value: number) => void;
  selectProps?: React.ComponentProps<typeof SearchSelect>[`SelectProps`];
  plans: T[];
}

export const GenericPlanSelector = <T extends GenericPlanType>({
  onChange,
  value,
  selectProps,
  onPlanChange,
  plans,
}: GenericPlanSelector<T>) => {
  const selectItems = plans.map(
    (plan) =>
      ({
        value: plan.id,
        name: plan.name,
      } satisfies SelectItem<number>)
  );

  function handleChange(planId: number) {
    if (plans) {
      const plan = plans.find((plan) => plan.id === planId);
      if (plan) {
        onChange?.(planId);
        onPlanChange?.(plan);
      }
    }
  }

  return (
    <SearchSelect
      dataCy="administration-plan-selector"
      SelectProps={selectProps}
      items={selectItems}
      value={value}
      onChange={handleChange}
    />
  );
};
