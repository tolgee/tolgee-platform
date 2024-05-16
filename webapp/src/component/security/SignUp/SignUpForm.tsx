import React from 'react';
import { Box, Link, Typography, styled } from '@mui/material';
import { LoadingButton } from '@mui/lab';
import { T, useTranslate } from '@tolgee/react';

import {
  LoadableType,
  StandardForm,
} from 'tg.component/common/form/StandardForm';
import { TextField } from 'tg.component/common/form/fields/TextField';
import { InvitationCodeService } from 'tg.service/InvitationCodeService';
import { Validation } from 'tg.constants/GlobalValidationSchema';
import { PasswordLabel } from '../SetPasswordField';
import { useConfig } from 'tg.globalContext/helpers';
import { ResourceErrorComponent } from '../../common/form/ResourceErrorComponent';
import { Alert } from '../../common/Alert';
import { SpendingLimitExceededDescription } from '../../billing/SpendingLimitExceeded';

const StyledInputFields = styled('div')`
  display: grid;
  align-items: start;
  padding-bottom: 12px;
`;

const PasswordFieldWithValidation = React.lazy(
  () => import('tg.component/security/PasswordFieldWithValidation')
);

export type SignUpType = {
  name: string;
  email: string;
  password: string;
  organizationName: string;
  invitationCode?: string;
};

type Props = {
  onSubmit: (v) => void;
  loadable: LoadableType;
};

const Error: React.FC<{ loadable: LoadableType }> = ({ loadable }) => {
  if (loadable.error?.code === 'seats_spending_limit_exceeded') {
    return (
      <Alert severity="error">
        <Typography variant="h5" sx={{ mb: 1 }}>
          <T keyName="spending_limit_dialog_title" />
        </Typography>
        <SpendingLimitExceededDescription />
      </Alert>
    );
  }

  if (loadable.error?.code === 'free_self_hosted_seat_limit_exceeded') {
    return (
      <Alert severity="error">
        <Typography variant="h5" sx={{ mb: 1 }}>
          <T keyName="free_self_hosted_seat_limit_exceeded" />
        </Typography>
      </Alert>
    );
  }

  return (
    <>
      {loadable && loadable.error && (
        <ResourceErrorComponent error={loadable.error} />
      )}
    </>
  );
};

export const SignUpForm = (props: Props) => {
  const config = useConfig();
  const orgRequired =
    !InvitationCodeService.getCode() && config.userCanCreateOrganizations;
  const { t } = useTranslate();

  return (
    <>
      <StandardForm
        rootSx={{ mb: 1 }}
        saveActionLoadable={props.loadable}
        showResourceError={false}
        initialValues={
          {
            password: '',
            name: '',
            email: '',
            organizationName: orgRequired ? '' : undefined,
          } as SignUpType
        }
        validationSchema={Validation.SIGN_UP(t, orgRequired)}
        submitButtons={
          <Box display="flex" flexDirection="column" alignItems="stretch">
            <LoadingButton
              data-cy="sign-up-submit-button"
              color="primary"
              type="submit"
              variant="contained"
              loading={props.loadable.isLoading}
            >
              <T keyName="sign_up_submit_button" />
            </LoadingButton>
          </Box>
        }
        onSubmit={props.onSubmit}
      >
        <StyledInputFields>
          <Error loadable={props.loadable} />
          <TextField
            name="name"
            label={<T keyName="sign_up_form_full_name" />}
          />
          <TextField name="email" label={<T keyName="sign_up_form_email" />} />
          {orgRequired && (
            <TextField
              name="organizationName"
              label={<T keyName="sign_up_form_organization_name" />}
            />
          )}
          <PasswordFieldWithValidation label={<PasswordLabel />} />
        </StyledInputFields>
      </StandardForm>
      <Box mt={1}>
        <Typography variant="caption" fontSize={14}>
          <T
            keyName="sign-up-terms-and-conditions-message"
            params={{
              Link: <Link href="https://tolgee.io/docs/terms_of_use" />,
            }}
          />
        </Typography>
      </Box>
    </>
  );
};
