import React, { FC } from 'react';
import { T, useTranslate } from '@tolgee/react';
import { Form, Formik } from 'formik';
import * as Yup from 'yup';
import {
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
} from '@mui/material';
import LoadingButton from 'tg.component/common/form/LoadingButton';

import { useBillingApiMutation } from 'tg.service/http/useQueryApi';
import { useMessage } from 'tg.hooks/useSuccessMessage';
import { getCloudPlanInitialValues } from '../../../subscriptionPlans/components/planForm/cloud/getCloudPlanInitialValues';
import { Validation } from 'tg.constants/GlobalValidationSchema';
import { useTestClock } from 'tg.service/useTestClock';
import { useCurrentDate } from 'tg.hooks/useCurrentDate';
import { CloudPlanFormData } from '../../../subscriptionPlans/components/planForm/cloud/types';
import { AssignCloudPlanFormFields } from './AssignCloudPlanFormFields';
import { components } from 'tg.service/billingApiSchema.generated';

export const AssignCloudPlanDialog: FC<{
  open: boolean;
  handleClose: () => void;
  organizationId: number;
  item: components['schemas']['OrganizationWithSubscriptionsModel'];
}> = ({ handleClose, open, organizationId, item }) => {
  const assignMutation = useBillingApiMutation({
    url: '/v2/administration/organizations/{organizationId}/billing/assign-cloud-plan',
    method: 'put',
    invalidatePrefix: '/v2/administration/billing',
  });

  const { t } = useTranslate();

  const messaging = useMessage();

  const isCurrentPlanPaid = item.cloudSubscription?.plan.free === false;
  const isTrialing = item.cloudSubscription?.status === 'TRIALING';
  const isCurrentlyPaying = isCurrentPlanPaid && !isTrialing;

  // It only makes sense to assign a plan if the organization is not currently paying,
  // If the organization is currently paying, the plan will be assigned, but it will make it visible for the organization
  const canAssignPlan = !isCurrentlyPaying;

  const handleSave = (
    value: AssignCloudPlanValuesType,
    { resetForm }: { resetForm: () => void }
  ) => {
    assignMutation.mutate(
      {
        path: { organizationId },
        content: {
          'application/json': {
            planId: value.customize ? undefined : value.planId!,
            trialEnd: value.trialEnd?.getTime(),
            customPlan: value.customize
              ? {
                  ...value.customPlan,
                  public: false,
                  stripeProductId: value.customPlan.stripeProductId ?? '',
                }
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

  const currentDate = useCurrentDate();

  return (
    <Dialog open={open} fullWidth maxWidth="lg" onClose={handleClose}>
      <Formik
        initialValues={
          {
            trialEnd: canAssignPlan ? currentDaPlus2weeks : undefined,
            planId: undefined,
            customPlan: cloudPlanInitialData,
            customize: false,
          } satisfies AssignCloudPlanValuesType
        }
        onSubmit={handleSave}
        validationSchema={Yup.object().shape({
          trialEnd: Yup.date().min(
            currentDate,
            t('date-must-be-in-future-error-message')
          ),
          planId: Yup.number().required(),
          customPlan: Validation.CLOUD_PLAN_FORM,
        })}
      >
        <Form>
          <DialogTitle>
            <T keyName="administration-subscription-assign-plan-dialog-title" />
          </DialogTitle>
          <DialogContent sx={{ display: 'grid', gap: '16px' }}>
            <AssignCloudPlanFormFields
              defaultTrialDate={currentDaPlus2weeks}
              organizationId={organizationId}
              isCurrentlyPaying={isCurrentlyPaying}
            />
          </DialogContent>
          <DialogActions>
            <Button onClick={handleClose}>
              <T keyName="global_cancel_button" />
            </Button>
            <LoadingButton
              type="submit"
              loading={assignMutation.isLoading}
              color="primary"
              variant="contained"
              data-cy="administration-assign-trial-assign-button"
            >
              <T keyName="administartion_billing_assign-trial-save_button" />
            </LoadingButton>
          </DialogActions>
        </Form>
      </Formik>
    </Dialog>
  );
};

export type AssignCloudPlanValuesType = {
  planId: number | undefined;
  trialEnd: Date | undefined;
  customPlan: CloudPlanFormData;
  customize: boolean;
};
