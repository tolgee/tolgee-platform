import { useBillingApiQuery } from 'tg.service/http/useQueryApi';
import { useField } from 'formik';
import { useFieldError } from 'tg.component/common/form/fields/useFieldError';
import {
  SearchSelect,
  SelectItem,
} from 'tg.component/searchSelect/SearchSelect';
import React, { FC } from 'react';
import { components } from 'tg.service/billingApiSchema.generated';
import { useTranslate } from '@tolgee/react';

type Props = {
  organizationId?: number;
  onPlanChange?: (
    plan: components['schemas']['AdministrationCloudPlanModel']
  ) => void;
};

export const PlanSelectorField = ({ organizationId, onPlanChange }: Props) => {
  const fieldName = 'planId';
  const [field, _, helpers] = useField(fieldName);
  const { errorTextWhenTouched } = useFieldError({ fieldName });

  const { t } = useTranslate();

  function onChange(planId) {
    helpers.setValue(planId);
  }

  return (
    <PlanSelector
      value={field.value}
      onChange={onChange}
      onPlanChange={onPlanChange}
      organizationId={organizationId}
      selectProps={{
        error: errorTextWhenTouched,
        label: t('administration_billing_trial_plan_assign_plan_label'),
      }}
    />
  );
};

export const PlanSelector: FC<
  Props & {
    value?: number;
    onChange?: (value: number) => void;
    selectProps?: React.ComponentProps<typeof SearchSelect>[`SelectProps`];
  }
> = ({ organizationId, onChange, value, selectProps, onPlanChange }) => {
  const plansLoadable = useBillingApiQuery({
    url: '/v2/administration/billing/cloud-plans',
    method: 'get',
    query: {
      filterAssignableToOrganization: organizationId,
    },
  });

  if (plansLoadable.isLoading) {
    return null;
  }

  const plans = plansLoadable?.data?._embedded?.plans ?? [];

  const selectItems = plans.map(
    (plan) =>
      ({
        value: plan.id,
        name: plan.name,
      } satisfies SelectItem<number>)
  );

  function handleChange(planId: number) {
    if (plansLoadable.data?._embedded?.plans) {
      const plan = plansLoadable.data._embedded.plans.find(
        (plan) => plan.id === planId
      );
      if (plan) {
        onChange?.(planId);
        onPlanChange?.(plan);
      }
    }
  }

  return (
    <SearchSelect
      SelectProps={selectProps}
      items={selectItems}
      value={value}
      onChange={handleChange}
    />
  );
};
