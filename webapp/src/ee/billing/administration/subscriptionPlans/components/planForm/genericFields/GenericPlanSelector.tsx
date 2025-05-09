import {
  SearchSelect,
  SelectItem,
} from 'tg.component/searchSelect/SearchSelect';
import React from 'react';
import { T } from '@tolgee/react';
import { Box } from '@mui/material';

type GenericPlanType = { id: number; name: string };

export interface GenericPlanSelector<T extends GenericPlanType> {
  organizationId?: number;
  onPlanChange?: (planId: T) => void;
  value?: number;
  onChange?: (value: number) => void;
  selectProps?: React.ComponentProps<typeof SearchSelect>[`SelectProps`];
  plans?: T[];
}

export const GenericPlanSelector = <T extends GenericPlanType>({
  onChange,
  value,
  selectProps,
  onPlanChange,
  plans,
}: GenericPlanSelector<T>) => {
  if (!plans) {
    return (
      <Box>
        <T keyName="administration-assign-plan-no-plans-to-assign" />
      </Box>
    );
  }

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

  const customCompareFunction = (prompt: string, label: string) => {
    return label.toLowerCase().includes(prompt.toLocaleLowerCase());
  };

  return (
    <SearchSelect
      dataCy="administration-plan-selector"
      SelectProps={selectProps}
      items={selectItems}
      value={value}
      onChange={handleChange}
      compareFunction={customCompareFunction}
    />
  );
};
