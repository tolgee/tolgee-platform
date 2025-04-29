import React, { FC } from 'react';
import { Alert, FormHelperText } from '@mui/material';
import { AssignTrialDatePickerField } from './fields/AssignTrialDatePickerField';
import { T, useTranslate } from '@tolgee/react';
import { AssignCloudPlanSelectorField } from './fields/AssignCloudPlanSelectorField';
import { CloudPlanFields } from '../../../../subscriptionPlans/components/planForm/cloud/fields/CloudPlanFields';
import { AssignCloudPlanValuesType } from './AssignCloudPlanDialog';
import { FormikProps, useFormikContext } from 'formik';
import { components } from 'tg.service/billingApiSchema.generated';
import { getCloudPlanInitialValues } from '../../../../subscriptionPlans/components/planForm/cloud/getCloudPlanInitialValues';
import { Switch } from 'tg.component/common/form/fields/Switch';
import { AssignCloudPlanTrialSwitchField } from './fields/AssignCloudPlanTrialSwitchField';

type AssignCloudPlanFormProps = {
  defaultTrialDate: Date;
  organizationId: number;
  isCurrentlyPaying: boolean;
};

export const AssignCloudPlanFormFields: FC<AssignCloudPlanFormProps> = ({
  defaultTrialDate,
  organizationId,
  isCurrentlyPaying,
}) => {
  const formikProps = useFormikContext<AssignCloudPlanValuesType>();

  const { t } = useTranslate();

  function setCustomPlanValues(
    formikProps: FormikProps<any>,
    planData: components['schemas']['AdministrationCloudPlanModel']
  ) {
    formikProps.setFieldValue('customPlan', {
      ...getCloudPlanInitialValues(planData),
      name: 'Customized ' + planData.name,
    });
  }

  const [selectedPlan, setSelectedPlan] = React.useState<
    components['schemas']['AdministrationCloudPlanModel'] | null
  >(null);

  const isTrial = formikProps.values.trialEnd !== undefined;

  const willOnlyMakeItVisible =
    isCurrentlyPaying || (!selectedPlan?.free && !isTrial);

  return (
    <>
      <AssignCloudPlanTrialSwitchField
        isCurrentlyPaying={isCurrentlyPaying}
        defaultTrialDate={defaultTrialDate}
      />
      {formikProps.values.trialEnd !== undefined && (
        <>
          <AssignTrialDatePickerField />
          <FormHelperText>
            <T keyName="administration-subscription-assign-trial-plan-help" />
          </FormHelperText>
        </>
      )}
      <AssignCloudPlanSelectorField
        organizationId={organizationId}
        onPlanChange={(plan) => {
          setCustomPlanValues(formikProps, plan);
          setSelectedPlan(plan);
        }}
        // It doesn't make sense to show public plans when it will only make them visible. They are already visible.
        filterPublic={willOnlyMakeItVisible ? false : undefined}
      />
      <Switch
        label={t('administration_customize_plan_switch')}
        name="customize"
        data-cy="administration-customize-plan-switch"
      />
      {formikProps.values.customize && (
        <>
          <FormHelperText>
            <T keyName="administration-customize-trial-plan-help" />
          </FormHelperText>
          <CloudPlanFields
            parentName="customPlan"
            isUpdate={false}
            canEditPrices={true}
          />
        </>
      )}

      {isCurrentlyPaying ? (
        <Alert
          severity="info"
          data-cy="administration-assign-plan-dialog-organization-paying-alert"
        >
          <T keyName="assign-plan-cannot-assign-plan-dialog-form-helper-text" />
        </Alert>
      ) : (
        !selectedPlan?.free &&
        !isTrial && (
          <Alert
            severity="info"
            data-cy="administration-assign-plan-dialog-not-trial-not-free-alert"
          >
            <T keyName="assign-plan-cannot-assign-paid-plan-dialog-form-helper-text" />
          </Alert>
        )
      )}
    </>
  );
};
