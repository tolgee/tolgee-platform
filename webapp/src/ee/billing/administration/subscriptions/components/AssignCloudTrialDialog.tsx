import React, { FC } from 'react';
import { T, useTranslate } from '@tolgee/react';
import { Form, Formik, FormikProps } from 'formik';
import * as Yup from 'yup';
import {
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControlLabel,
  FormHelperText,
  Switch,
} from '@mui/material';
import { DateTimePickerField } from 'tg.component/common/form/fields/DateTimePickerField';
import LoadingButton from 'tg.component/common/form/LoadingButton';

import { useBillingApiMutation } from 'tg.service/http/useQueryApi';
import { useMessage } from 'tg.hooks/useSuccessMessage';
import { CloudPlanFields } from '../../subscriptionPlans/components/planForm/fields/CloudPlanFields';
import { getCloudPlanInitialValues } from '../../subscriptionPlans/components/planForm/getCloudPlanInitialValues';
import { components } from 'tg.service/billingApiSchema.generated';
import { Validation } from 'tg.constants/GlobalValidationSchema';
import { PlanSelectorField } from '../../subscriptionPlans/components/planForm/fields/PlanSelectorField';
import { CloudPlanFormData } from '../../subscriptionPlans/components/planForm/CloudPlanFormBase';
import { useTestClock } from 'tg.service/useTestClock';

export const AssignCloudTrialDialog: FC<{
  open: boolean;
  handleClose: () => void;
  organizationId: number;
}> = ({ handleClose, open, organizationId }) => {
  const assignMutation = useBillingApiMutation({
    url: '/v2/administration/organizations/{organizationId}/billing/assign-cloud-plan',
    method: 'put',
    invalidatePrefix: '/v2/administration/billing',
  });

  const { t } = useTranslate();

  const messaging = useMessage();

  const [customize, setCustomize] = React.useState(false);

  const handleSave = (
    value: ValuesType,
    { resetForm }: { resetForm: () => void }
  ) => {
    assignMutation.mutate(
      {
        path: { organizationId },
        content: {
          'application/json': {
            planId: customize ? undefined : value.planId!,
            trialEnd: value.trialEnd.getTime(),
            customPlan: customize
              ? { ...value.customPlan, public: false }
              : undefined,
          },
        },
      },
      {
        onSuccess() {
          handleClose();
          resetForm();
          messaging.success(
            <T keyName="administration-subscription-plan-assigned-success-message" />
          );
        },
      }
    );
  };

  const currentTimestamp = useTestClock() || Date.now();

  const currentDaPlus2weeks = new Date(
    currentTimestamp + 14 * 24 * 60 * 60 * 1000
  );

  const cloudPlanInitialData = getCloudPlanInitialValues();

  function setCustomPlanValues(
    formikProps: FormikProps<any>,
    planData: components['schemas']['AdministrationCloudPlanModel']
  ) {
    formikProps.setFieldValue('customPlan', {
      ...getCloudPlanInitialValues(planData),
      name: 'Customized ' + planData.name,
    });
  }

  return (
    <Formik
      initialValues={
        {
          trialEnd: currentDaPlus2weeks,
          planId: undefined,
          customPlan: cloudPlanInitialData,
        } satisfies ValuesType
      }
      onSubmit={handleSave}
      validationSchema={Yup.object().shape({
        trialEnd: Yup.date()
          .required()
          .min(new Date(), t('date-must-be-in-future-error-message')),
        planId: Yup.number().required(),
        customPlan: Validation.CLOUD_PLAN_FORM,
      })}
    >
      {(formikProps) => (
        <Form>
          <Dialog open={open} fullWidth maxWidth="lg" onClose={handleClose}>
            <DialogTitle>
              <T keyName="administration-subscription-assign-trial-dialog-title" />
            </DialogTitle>
            <DialogContent sx={{ display: 'grid', gap: '16px' }}>
              <DateTimePickerField
                formControlProps={{
                  'data-cy': 'administration-trial-end-date-field',
                  sx: { mt: 1 },
                }}
                dateTimePickerProps={{
                  disablePast: true,
                  label: (
                    <T keyName="administration-subscription-assign-trial-end-date-field" />
                  ),
                }}
                name="trialEnd"
              />
              <FormHelperText>
                <T keyName="administration-subscription-assign-trial-plan-help" />
              </FormHelperText>
              <PlanSelectorField
                organizationId={organizationId}
                onPlanChange={(plan) => setCustomPlanValues(formikProps, plan)}
              />
              <FormControlLabel
                control={
                  <Switch
                    checked={customize}
                    onChange={() => setCustomize(!customize)}
                  />
                }
                data-cy="administration-customize-plan-switch"
                label={t('administration_customize_plan_switch')}
              />
              {customize && (
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
            </DialogContent>
            <DialogActions>
              <Button onClick={handleClose}>
                {' '}
                <T keyName="global_cancel_button" />
              </Button>
              <LoadingButton
                type="submit"
                onClick={formikProps.submitForm}
                loading={assignMutation.isLoading}
                color="primary"
                variant="contained"
                data-cy="administration-assign-trial-assign-button"
              >
                <T keyName="administartion_billing_assign-trial-save_button" />
              </LoadingButton>
            </DialogActions>
          </Dialog>
        </Form>
      )}
    </Formik>
  );
};

type ValuesType = {
  planId: number | undefined;
  trialEnd: Date;
  customPlan: CloudPlanFormData;
};
