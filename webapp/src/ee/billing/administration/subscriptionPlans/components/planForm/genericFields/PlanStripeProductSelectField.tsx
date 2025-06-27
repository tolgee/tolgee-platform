import React, { FC } from 'react';
import { Field, FieldProps } from 'formik';
import { SearchSelect } from 'tg.component/searchSelect/SearchSelect';
import { useBillingApiQuery } from 'tg.service/http/useQueryApi';
import { useTranslate } from '@tolgee/react';
import { usePlanFormValues } from 'tg.ee.module/billing/administration/subscriptionPlans/components/planForm/cloud/usePlanFormValues';
import {
  CloudPlanFormData,
  SelfHostedEePlanFormData,
} from 'tg.ee.module/billing/administration/subscriptionPlans/components/planForm/cloud/types';

type StripeProductSelectFieldProps = {
  parentName?: string;
};

export const PlanStripeProductSelectField: FC<
  StripeProductSelectFieldProps
> = ({ parentName = '' }) => {
  const { values } = usePlanFormValues<
    CloudPlanFormData | SelfHostedEePlanFormData
  >(parentName);

  if (values.newStripeProduct) {
    return null;
  }
  const productsLoadable = useBillingApiQuery({
    url: '/v2/administration/billing/stripe-products',
    method: 'get',
  });

  const products = productsLoadable.data?._embedded?.stripeProducts;

  const { t } = useTranslate();

  return (
    <Field name={`${parentName}stripeProductId`}>
      {({ field, form, meta }: FieldProps) => {
        return (
          <SearchSelect
            compareFunction={(prompt, label) =>
              label.toLowerCase().includes(prompt.toLowerCase())
            }
            minHeight={true}
            SelectProps={{
              // @ts-ignore
              'data-cy': 'administration-plan-field-stripe-product',
              label: t('administration_cloud_plan_field_stripe_product'),
              size: 'small',
              fullWidth: true,
              variant: 'outlined',
              error: (meta.touched && meta.error) || '',
            }}
            value={field.value}
            onChange={(val) => form.setFieldValue(field.name, val)}
            items={[
              { value: '', name: 'None' },
              ...(!products?.find((product) => product.id === field.value) &&
              field.value
                ? [
                    {
                      value: field.value,
                      name: `${field.value}`,
                    },
                  ]
                : []),
              ...(products?.map(({ id, name }) => ({
                value: id,
                name: `${name} (${id})`,
              })) || []),
            ]}
          />
        );
      }}
    </Field>
  );
};
