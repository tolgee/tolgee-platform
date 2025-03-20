import { Box } from '@mui/material';
import { Form, Formik } from 'formik';
import { Validation } from 'tg.constants/GlobalValidationSchema';
import { PlanEnabledFeaturesField } from '../genericFields/PlanEnabledFeaturesField';
import { PlanIncludedUsageFields } from '../genericFields/PlanIncludedUsageFields';
import { PlanNonCommercialSwitch } from '../genericFields/PlanNonCommercialSwitch';
import React from 'react';
import { PlanPricesFields } from '../genericFields/PlanPricesFields';
import { PlanStripeProductSelectField } from '../genericFields/PlanStripeProductSelectField';
import { PlanFreePlanSwitch } from '../genericFields/PlanFreePlanSwitch';
import { PlanPublicSwitchField } from '../genericFields/PlanPublicSwitchField';
import { PlanNameField } from '../genericFields/PlanNameField';
import { SelfHostedEePlanFormData } from '../cloud/types';
import { SelfHostedEePlanTypeSelectField } from './SelfHostedEePlanTypeSelectField';
import { PlanSaveButton } from '../genericFields/PlanSaveButton';

type Props = {
  initialData: SelfHostedEePlanFormData;
  onSubmit: (value: SelfHostedEePlanFormData) => void;
  loading: boolean | undefined;
  isUpdate: boolean;
};

export function EePlanForm({
  initialData,
  onSubmit,
  loading,
  isUpdate,
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
          <PlanPricesFields isPayAsYouGo={initialData.isPayAsYouGo} />
          <PlanIncludedUsageFields />
          <PlanEnabledFeaturesField />
          <PlanNonCommercialSwitch />
          <PlanSaveButton loading={loading} />
        </Box>
      </Form>
    </Formik>
  );
}
