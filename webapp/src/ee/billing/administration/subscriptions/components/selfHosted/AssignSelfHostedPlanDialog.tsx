import React, { FC } from 'react';
import { Alert, Dialog } from '@mui/material';
import { Formik } from 'formik';
import * as Yup from 'yup';
import { AssignCloudPlanDialogForm } from '../generic/assignPlan/AssignCloudPlanDialogForm';
import { components } from 'tg.service/billingApiSchema.generated';
import { useBillingApiMutation } from 'tg.service/http/useQueryApi';
import { SelfHostedPlanSelectorField } from './SelfHostedPlanSelectorField';
import { T } from '@tolgee/react';
import { useMessage } from 'tg.hooks/useSuccessMessage';

type AssignSelfHostedPlanDialogProps = {
  open: boolean;
  onClose: () => void;
  item: components['schemas']['OrganizationWithSubscriptionsModel'];
};

export const AssignSelfHostedPlanDialog: FC<
  AssignSelfHostedPlanDialogProps
> = ({ open, item, onClose }) => {
  const assignMutation = useBillingApiMutation({
    url: '/v2/administration/organizations/{organizationId}/billing/assign-self-hosted-plan',
    method: 'put',
    invalidatePrefix: '/v2/administration/billing',
  });

  const messaging = useMessage();

  const handleSave = (value, { resetForm }: { resetForm: () => void }) => {
    assignMutation.mutate(
      {
        path: { organizationId: item.organization.id },
        content: {
          'application/json': {
            ...value,
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
  const [selectedPlan, setSelectedPlan] = React.useState<
    components['schemas']['SelfHostedEePlanModel'] | null
  >(null);

  return (
    <Dialog open={open} fullWidth maxWidth="lg" onClose={onClose}>
      <Formik
        initialValues={{
          planId: 0,
        }}
        onSubmit={handleSave}
        validationSchema={Yup.object().shape({
          planId: Yup.number().required().min(0, 'Required'),
        })}
      >
        <AssignCloudPlanDialogForm
          fields={
            <>
              <SelfHostedPlanSelectorField
                onPlanChange={(plan) => setSelectedPlan(plan)}
              />
              {!selectedPlan?.free && (
                <Alert severity="info">
                  <T keyName="administration-assign-subscription-self-hosted-plan-assign-plan-not-free-alert" />
                </Alert>
              )}
            </>
          }
          onClose={onClose}
          saveMutation={assignMutation}
        />
      </Formik>
    </Dialog>
  );
};
