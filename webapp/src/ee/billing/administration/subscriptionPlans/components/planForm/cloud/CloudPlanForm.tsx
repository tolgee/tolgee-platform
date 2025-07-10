import { Box } from '@mui/material';
import { CloudPlanFields } from './fields/CloudPlanFields';
import React, { ComponentProps, ReactNode } from 'react';
import { PlanPublicSwitchField } from '../genericFields/PlanPublicSwitchField';
import { PlanSaveButton } from '../genericFields/PlanSaveButton';
import { CloudPlanFormData } from './types';
import { useUrlSearch } from 'tg.hooks/useUrlSearch';
import { Form, Formik } from 'formik';
import { Validation } from 'tg.constants/GlobalValidationSchema';
import { PlanArchivedSwitch } from 'tg.ee.module/billing/administration/subscriptionPlans/components/planForm/genericFields/PlanArchivedSwitch';

type Props = {
  isUpdate?: boolean;
  initialData: CloudPlanFormData;
  onSubmit: (value: CloudPlanFormData) => void;
  loading: boolean | undefined;
  canEditPrices: boolean;
  beforeFields?: ReactNode;
  publicSwitchFieldProps?: ComponentProps<typeof PlanPublicSwitchField>;
};

export function CloudPlanForm({
  isUpdate,
  initialData,
  loading,
  canEditPrices,
  onSubmit,
  beforeFields,
  publicSwitchFieldProps,
}: Props) {
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
      <Form>
        {beforeFields}
        <Box mb={3} pt={2}>
          <Box display="grid">
            <PlanArchivedSwitch isUpdate={isUpdate} />
            <PlanPublicSwitchField {...publicSwitchFieldProps} />
          </Box>
          <CloudPlanFields isUpdate={isUpdate} canEditPrices={canEditPrices} />

          <PlanSaveButton loading={loading} />
        </Box>
      </Form>
    </Formik>
  );
}
