import React, { FC } from 'react';
import { T, useTranslate } from '@tolgee/react';
import { Formik } from 'formik';
import * as Yup from 'yup';
import { Dialog } from '@mui/material';

import { useBillingApiMutation } from 'tg.service/http/useQueryApi';
import { useMessage } from 'tg.hooks/useSuccessMessage';
import { getCloudPlanInitialValues } from '../../../../subscriptionPlans/components/planForm/cloud/getCloudPlanInitialValues';
import { Validation } from 'tg.constants/GlobalValidationSchema';
import { useTestClock } from 'tg.service/useTestClock';
import { useCurrentDate } from 'tg.hooks/useCurrentDate';
import { CloudPlanFormData } from '../../../../subscriptionPlans/components/planForm/cloud/types';
import { AssignCloudPlanFormFields } from './AssignCloudPlanFormFields';
import { components } from 'tg.service/billingApiSchema.generated';
import { AssignCloudPlanDialogForm } from '../../generic/assignPlan/AssignCloudPlanDialogForm';

export const AssignCloudPlanDialog: FC<{
  open: boolean;
  onClose: () => void;
  item: components['schemas']['OrganizationWithSubscriptionsModel'];
}> = ({ onClose, open, item }) => {
  const assignMutation = useBillingApiMutation({
    url: '/v2/administration/organizations/{organizationId}/billing/assign-cloud-plan',
    method: 'put',
    invalidatePrefix: ['/v2/administration/billing', '/v2/organizations'],
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
        path: { organizationId: item.organization.id },
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
          onClose();
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
    <Dialog open={open} fullWidth maxWidth="lg" onClose={onClose}>
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
        <AssignCloudPlanDialogForm
          fields={
            <AssignCloudPlanFormFields
              defaultTrialDate={currentDaPlus2weeks}
              organizationId={item.organization.id}
              isCurrentlyPaying={isCurrentlyPaying}
            />
          }
          onClose={onClose}
          saveMutation={assignMutation}
        />
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
