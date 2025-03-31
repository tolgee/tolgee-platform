import { Box } from '@mui/material';
import { Form, Formik } from 'formik';
import { Validation } from 'tg.constants/GlobalValidationSchema';
import { PlanEnabledFeaturesField } from '../genericFields/PlanEnabledFeaturesField';
import { PlanIncludedUsageFields } from '../genericFields/PlanIncludedUsageFields';
import { PlanNonCommercialSwitch } from '../genericFields/PlanNonCommercialSwitch';
import React, { ReactElement } from 'react';
import { PlanStripeProductSelectField } from '../genericFields/PlanStripeProductSelectField';
import { PlanFreePlanSwitch } from '../genericFields/PlanFreePlanSwitch';
import { PlanPublicSwitchField } from '../genericFields/PlanPublicSwitchField';
import { PlanNameField } from '../genericFields/PlanNameField';
import { SelfHostedEePlanFormData } from '../cloud/types';
import { SelfHostedEePlanTypeSelectField } from './fields/SelfHostedEePlanTypeSelectField';
import { PlanSaveButton } from '../genericFields/PlanSaveButton';
import { PlanPricesFields } from '../genericFields/PlanPricesFields';

type Props = {
  initialData: SelfHostedEePlanFormData;
  onSubmit: (value: SelfHostedEePlanFormData) => void;
  loading: boolean | undefined;
  isUpdate: boolean;
  beforeFields?: ReactElement;
};

export function SelfHostedEePlanForm({
  initialData,
  onSubmit,
  loading,
  isUpdate,
  beforeFields,
}: Props) {
  return (
    <Formik
      initialValues={initialData}
      enableReinitialize
      onSubmit={onSubmit}
      validationSchema={Validation.EE_PLAN_FORM}
    >
      <Form>
        <Box mb={3} mt={3}>
          <PlanPublicSwitchField />

          {beforeFields}

          <PlanNameField />
          <PlanFreePlanSwitch isUpdate={isUpdate} />
          <Box
            sx={{
              display: 'grid',
              gap: 2,
              mt: 2,
              gridTemplateColumns: '1fr 1fr',
            }}
          >
            <SelfHostedEePlanTypeSelectField />
            <PlanStripeProductSelectField />
          </Box>
          <PlanPricesFields />
          <PlanIncludedUsageFields />
          <PlanEnabledFeaturesField />
          <PlanNonCommercialSwitch />
          <PlanSaveButton loading={loading} />
        </Box>
      </Form>
    </Formik>
  );
}
