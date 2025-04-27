import React, { FC, useState } from 'react';
import { useTranslate } from '@tolgee/react';
import { useFormikContext } from 'formik';
import { Divider, FormHelperText } from '@mui/material';
import { SelfHostedEePlanSelector } from './SelfHostedEePlanSelector';
import { getSelfHostedPlanInitialValues } from '../getSelfHostedPlanInitialValues';
import { SelfHostedEePlanFormData } from '../../cloud/types';

export const SelfHostedEePlanTemplateSelectorField: FC = () => {
  const { t } = useTranslate();

  const { setValues, values } = useFormikContext<SelfHostedEePlanFormData>();

  const [selectedPlanId, setSelectedPlanId] = useState<number | undefined>(
    undefined
  );

  return (
    <>
      <SelfHostedEePlanSelector
        value={selectedPlanId}
        selectProps={{
          label: t('admin_billing_create_plan_template_select'),
        }}
        onPlanChange={(plan) => {
          const newValues = {
            ...getSelfHostedPlanInitialValues(plan),
            name: values.name,
            // usually, we don't want to create public plans.
            // also, usually it's a good idea to create private plans first
            public: false,
          };
          setValues(newValues);
          setSelectedPlanId(plan.id);
        }}
      />
      <FormHelperText sx={{ mb: 3 }}>
        {t('admin_billing_plan_template_selector_helper_text')}
      </FormHelperText>
      <Divider />
    </>
  );
};
