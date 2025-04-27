import React, { FC } from 'react';
import { SelfHostedEePlanSelector } from '../../../subscriptionPlans/components/planForm/selfHostedEe/fields/SelfHostedEePlanSelector';
import { useField, useFormikContext } from 'formik';
import { useTranslate } from '@tolgee/react';
import { SelfHostedEePlanModel } from '../../../../Subscriptions/selfHosted/PlansSelfHostedList';

type SelfHostedPlanSelectorFieldProps = {
  onPlanChange?: (plan: SelfHostedEePlanModel) => void;
};

export const SelfHostedPlanSelectorField: FC<
  SelfHostedPlanSelectorFieldProps
> = ({ onPlanChange: onPlanChangeProp }) => {
  const { t } = useTranslate();
  const name = 'planId';
  const [field, meta] = useField(name);
  const context = useFormikContext();

  function onPlanChange(plan: SelfHostedEePlanModel) {
    context.setFieldValue(name, plan.id);
    onPlanChangeProp?.(plan);
  }

  return (
    <SelfHostedEePlanSelector
      value={field.value}
      onPlanChange={onPlanChange}
      selectProps={{
        error: meta.error,
        label: t('administration_billing_trial_plan_assign_plan_label'),
      }}
    />
  );
};
