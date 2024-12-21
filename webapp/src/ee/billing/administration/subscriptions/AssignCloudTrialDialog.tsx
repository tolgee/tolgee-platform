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
import { DateTimePickerField } from 'tg.component/common/form/fields/DateTimePickerField';
import LoadingButton from 'tg.component/common/form/LoadingButton';

import { PlanSelectorField } from './CloudPlanSelector';
import { useBillingApiMutation } from 'tg.service/http/useQueryApi';
import { useMessage } from 'tg.hooks/useSuccessMessage';

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

  function handleSave(value: ValuesType) {
    assignMutation.mutate(
      {
        path: { organizationId },
        content: {
          'application/json': {
            planId: value.planId!,
            trialEnd: value.trialEnd.getTime(),
          },
        },
      },
      {
        onSuccess() {
          handleClose();
          messaging.success(
            <T keyName="administration-subscription-plan-assigned-success-message" />
          );
        },
      }
    );
  }

  const currentDaPlus2weeks = new Date(Date.now() + 1000 * 60 * 60 * 24 * 14);

  return (
    <Formik
      initialValues={
        {
          trialEnd: currentDaPlus2weeks,
          planId: undefined,
        } satisfies ValuesType
      }
      onSubmit={handleSave}
      validationSchema={Yup.object().shape({
        trialEnd: Yup.date()
          .required()
          .min(new Date(), t('date-must-be-in-future-error-message')),
        planId: Yup.number().required(),
      })}
    >
      {(formikProps) => (
        <Form>
          <Dialog open={open} fullWidth maxWidth="sm" onClose={handleClose}>
            <DialogTitle>
              <T keyName="administration-subscription-assign-trial-dialog-title" />
            </DialogTitle>
            <DialogContent sx={{ display: 'grid', gap: '16px' }}>
              <DateTimePickerField
                formControlProps={{ sx: { mt: 1 } }}
                dateTimePickerProps={{
                  disablePast: true,
                  label: (
                    <T keyName="administration-subscription-assign-trial-end-date-field" />
                  ),
                }}
                name="trialEnd"
              />
              <PlanSelectorField organizationId={organizationId} />
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
                data-cy="project-ai-prompt-dialog-save"
              >
                <T keyName="global_form_save" />
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
};
