import React, { FC, useState } from 'react';
import { useTranslate } from '@tolgee/react';
import { useFormikContext } from 'formik';
import { PlanSelector } from './PlanSelectorField';
import { getCloudPlanInitialValues } from '../getCloudPlanInitialValues';
import { Divider, FormHelperText } from '@mui/material';
import { CloudPlanFormData } from '../CloudPlanFormBase';

export const PlanTemplateSelector: FC = () => {
  const { t } = useTranslate();

  const { setValues, values } = useFormikContext<CloudPlanFormData>();

  const [selectedPlanId, setSelectedPlanId] = useState<number | undefined>(
    undefined
  );

  return (
    <>
      <PlanSelector
        value={selectedPlanId}
        selectProps={{
          label: t('admin_billing_create_plan_template_select'),
        }}
        onPlanChange={(plan) => {
          const newValues = {
            ...getCloudPlanInitialValues(plan),
            name: values.name,
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
