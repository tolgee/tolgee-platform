import { useBillingApiQuery } from 'tg.service/http/useQueryApi';
import { useField } from 'formik';
import { useFieldError } from 'tg.component/common/form/fields/useFieldError';
import {
  SearchSelect,
  SelectItem,
} from 'tg.component/searchSelect/SearchSelect';
import React from 'react';

export const PlanSelectorField = ({
  organizationId,
}: {
  organizationId: number;
}) => {
  const plansLoadable = useBillingApiQuery({
    url: '/v2/administration/billing/cloud-plans',
    method: 'get',
    query: {
      filterAssignableToOrganization: organizationId,
    },
  });

  const fieldName = 'planId';
  const [field, _, helpers] = useField(fieldName);
  const { errorTextWhenTouched } = useFieldError({ fieldName });

  if (plansLoadable.isLoading) {
    return null;
  }

  function onChange(val) {
    helpers.setValue(val);
  }

  const plans = plansLoadable?.data?._embedded?.plans ?? [];
  const selectItems = plans.map(
    (plan) =>
      ({
        value: plan.id,
        name: plan.name,
      } satisfies SelectItem<number>)
  );

  return (
    <SearchSelect
      SelectProps={{ error: errorTextWhenTouched }}
      items={selectItems}
      value={field.value}
      onChange={onChange}
    />
  );
};
