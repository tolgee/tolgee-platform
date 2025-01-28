import { Form, Formik } from 'formik';
import { components } from 'tg.service/billingApiSchema.generated';
import { Validation } from 'tg.constants/GlobalValidationSchema';
import { useUrlSearch } from 'tg.hooks/useUrlSearch';
import React from 'react';

type CloudPlanModel = components['schemas']['CloudPlanRequest'];
type EnabledFeature =
  components['schemas']['CloudPlanRequest']['enabledFeatures'][number];

export type CloudPlanFormData = {
  type: CloudPlanModel['type'];
  name: string;
  prices: CloudPlanModel['prices'];
  includedUsage: CloudPlanModel['includedUsage'];
  stripeProductId: string;
  enabledFeatures: EnabledFeature[];
  forOrganizationIds: number[];
  public: boolean;
  free: boolean;
  nonCommercial: boolean;
};

type Props = {
  children: React.ReactNode;
  initialData: CloudPlanFormData;
  onSubmit: (value: CloudPlanFormData) => void;
};

export function CloudPlanFormBase({ initialData, children, onSubmit }: Props) {
  const { creatingForOrganizationId: creatingForOrganizationIdString } =
    useUrlSearch();

  const creatingForOrganizationId = creatingForOrganizationIdString
    ? parseInt(creatingForOrganizationIdString as string)
    : undefined;

  return (
    <Formik
      initialValues={
        {
          ...initialData,
          public: initialData.public && !creatingForOrganizationId,
        } as CloudPlanFormData
      }
      enableReinitialize
      onSubmit={(values) => {
        let prices = values.prices;
        if (values.type !== 'PAY_AS_YOU_GO') {
          prices = {
            subscriptionMonthly: values.prices.subscriptionMonthly,
            subscriptionYearly: values.prices.subscriptionYearly,
          };
        }

        const forOrganizationIds = creatingForOrganizationId
          ? [creatingForOrganizationId]
          : values.forOrganizationIds;

        onSubmit({ ...values, prices, forOrganizationIds });
      }}
      validationSchema={Validation.CLOUD_PLAN_FORM}
    >
      <Form>{children}</Form>
    </Formik>
  );
}
