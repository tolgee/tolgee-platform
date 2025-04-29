import { useField } from 'formik';
import { useFieldError } from 'tg.component/common/form/fields/useFieldError';
import React from 'react';
import { components } from 'tg.service/billingApiSchema.generated';
import { useTranslate } from '@tolgee/react';
import { CloudPlanSelector } from '../../../../../subscriptionPlans/components/planForm/cloud/fields/CloudPlanSelector';

type CloudPlanSelectorFieldProps = {
  organizationId?: number;
  onPlanChange?: (
    plan: components['schemas']['AdministrationCloudPlanModel']
  ) => void;
  filterPublic?: boolean;
};

export const AssignCloudPlanSelectorField = ({
  organizationId,
  onPlanChange,
  filterPublic,
}: CloudPlanSelectorFieldProps) => {
  const fieldName = 'planId';
  const [field, _, helpers] = useField(fieldName);
  const { errorTextWhenTouched } = useFieldError({ fieldName });

  const { t } = useTranslate();

  function onChange(planId) {
    helpers.setValue(planId);
  }

  return (
    <CloudPlanSelector
      value={field.value}
      onChange={onChange}
      onPlanChange={onPlanChange}
      organizationId={organizationId}
      selectProps={{
        error: errorTextWhenTouched,
        label: t('administration_billing_trial_plan_assign_plan_label'),
      }}
      filterPublic={filterPublic}
    />
  );
};
