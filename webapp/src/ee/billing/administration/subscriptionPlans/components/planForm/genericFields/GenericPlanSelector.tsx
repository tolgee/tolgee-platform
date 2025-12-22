import {
  SearchSelect,
  SelectItem,
} from 'tg.component/searchSelect/SearchSelect';
import React from 'react';
import { T } from '@tolgee/react';
import { Box } from '@mui/material';
import { BoxLoading } from 'tg.component/common/BoxLoading';
import { useUserPreferenceStorage } from 'tg.hooks/useUserPreferenceStorage';

type GenericPlanType = { id: number; name: string };

export interface GenericPlanSelector<T extends GenericPlanType> {
  organizationId?: number;
  onPlanChange?: (planId: T) => void;
  value?: number;
  onChange?: (value: number) => void;
  selectProps?: React.ComponentProps<typeof SearchSelect>[`SelectProps`];
  plans?: T[];
  loading: boolean;
}

export const GenericPlanSelector = <T extends GenericPlanType>({
  onChange,
  value,
  selectProps,
  onPlanChange,
  plans,
  loading,
}: GenericPlanSelector<T>) => {
  const sortedPlans = useSortPlans(plans);

  const selectItems =
    sortedPlans?.map(
      (plan) =>
        ({
          value: plan.id,
          name: plan.name,
        } satisfies SelectItem<number>)
    ) || [];

  const { incrementPlanWithId } = usePreferredPlans();

  if (loading) {
    return <BoxLoading />;
  }

  if (!sortedPlans) {
    return (
      <Box>
        <T keyName="administration-assign-plan-no-plans-to-assign" />
      </Box>
    );
  }

  function handleChange(planId: number) {
    if (sortedPlans) {
      const plan = sortedPlans.find((plan) => plan.id === planId);
      if (plan) {
        onChange?.(planId);
        onPlanChange?.(plan as T);
        incrementPlanWithId(planId);
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

/**
 * Sorts plans by user's preferred plans.
 * The purpose of this is to put the user's popular plans to the top.
 */
function useSortPlans(plans?: GenericPlanType[]) {
  const { preferredPlansLoadable } = usePreferredPlans();

  return React.useMemo(() => {
    if (!plans) {
      return undefined;
    }

    return [...plans].sort(
      (a, b) =>
        (preferredPlansLoadable.data?.data?.[b.id] || 0) -
        (preferredPlansLoadable.data?.data?.[a.id] || 0)
    );
  }, [plans, preferredPlansLoadable.data]);
}

/**
 * Returns a user's preferred plans and a function to increment a plan's count.
 *
 * The setting is stored on the server in the storageJson filed on the UserPreference entity.
 */
function usePreferredPlans() {
  const { loadable, update } = useUserPreferenceStorage(
    'billingAdminPreferredPlans'
  );

  return {
    preferredPlansLoadable: loadable,
    incrementPlanWithId: async (planId: number) => {
      const refetched = await loadable.refetch();
      const current = refetched.data?.data?.[planId] ?? 0;
      const newValue = {
        ...refetched.data,
        [planId]: current + 1,
      };

      update(newValue);
    },
  };
}
