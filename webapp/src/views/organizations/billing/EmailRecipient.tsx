import { useOrganization } from '../useOrganization';
import {
  useBillingApiMutation,
  useBillingApiQuery,
} from 'tg.service/http/useQueryApi';
import { T } from '@tolgee/react';
import { StandardForm } from 'tg.component/common/form/StandardForm';
import { TextField } from 'tg.component/common/form/fields/TextField';
import { Validation } from 'tg.constants/GlobalValidationSchema';
import { LoadingButton } from '@mui/lab';
import { useMessage } from 'tg.hooks/useSuccessMessage';
import { Box } from '@mui/material';

export const EmailRecipient = () => {
  const organization = useOrganization();
  const message = useMessage();

  const billingDataLoadable = useBillingApiQuery({
    url: '/v2/organizations/{organizationId}/billing/billing-info',
    method: 'get',
    path: {
      organizationId: organization!.id,
    },
  });

  const updateMutation = useBillingApiMutation({
    url: '/v2/organizations/{organizationId}/billing/email-recipient',
    method: 'put',
    options: {
      onSuccess() {
        message.success(
          <T>billing_email_recipient_successfully_saved_message</T>
        );
      },
    },
  });

  return (
    <>
      {billingDataLoadable.data?.emailRecipient && (
        <StandardForm
          loading={billingDataLoadable.isLoading}
          validationSchema={Validation.BILLING_RECIPIENT_EMAIL}
          initialValues={{
            emailRecipient: billingDataLoadable.data?.emailRecipient,
          }}
          onSubmit={(v) => {
            updateMutation.mutate({
              path: { organizationId: organization!.id },
              content: { 'application/json': { email: v.emailRecipient } },
            });
          }}
          submitButtons={<></>}
        >
          {(formikProps) => {
            return (
              <Box display="flex">
                <Box mr={2}>
                  <TextField
                    fullWidth={false}
                    label={
                      <T keyName="billing_recipient_email">
                        Invoice recipient e-mail
                      </T>
                    }
                    name="emailRecipient"
                  />
                </Box>
                <LoadingButton
                  variant="contained"
                  color="primary"
                  type="submit"
                  loading={updateMutation.isLoading}
                  disabled={!formikProps.dirty}
                >
                  <T keyName="global_form_save" />
                </LoadingButton>
              </Box>
            );
          }}
        </StandardForm>
      )}
    </>
  );
};
