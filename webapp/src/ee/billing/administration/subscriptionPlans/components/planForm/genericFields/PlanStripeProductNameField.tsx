import React, { FC } from 'react';
import { TextField } from 'tg.component/common/form/fields/TextField';
import { useTranslate } from '@tolgee/react';
import { usePlanFormValues } from 'tg.ee.module/billing/administration/subscriptionPlans/components/planForm/cloud/usePlanFormValues';
import {
  CloudPlanFormData,
  SelfHostedEePlanFormData,
} from 'tg.ee.module/billing/administration/subscriptionPlans/components/planForm/cloud/types';

type Props = {
  parentName?: string;
};

export const PlanStripeProductNameField: FC<Props> = ({ parentName = '' }) => {
  const { t } = useTranslate();
  const { values } = usePlanFormValues<
    CloudPlanFormData | SelfHostedEePlanFormData
  >(parentName);

  if (!values.newStripeProduct) {
    return null;
  }
  return (
    <TextField
      name={`${parentName}stripeProductName`}
      size="small"
      label={t('administration_cloud_plan_field_stripe_product_name')}
      fullWidth
      data-cy="administration-plan-field-stripe-product-name"
    />
  );
};
