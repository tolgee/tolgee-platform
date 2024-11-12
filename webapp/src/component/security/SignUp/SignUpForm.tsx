import React, { useState } from 'react';
import {
  Box,
  IconButton,
  Link,
  Tooltip,
  Typography,
  styled,
} from '@mui/material';
import { LoadingButton } from '@mui/lab';
import { T, useTranslate } from '@tolgee/react';
import { Eye, EyeOff } from '@untitled-ui/icons-react';

import {
  LoadableType,
  StandardForm,
} from 'tg.component/common/form/StandardForm';
import { TextField } from 'tg.component/common/form/fields/TextField';
import { InvitationCodeService } from 'tg.service/InvitationCodeService';
import { Validation } from 'tg.constants/GlobalValidationSchema';
import { useConfig } from 'tg.globalContext/helpers';
import { PasswordLabel } from '../SetPasswordField';
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
  userSource?: string;
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
  const userSourceField = config.userSourceField;

  const [showPassword, setShowPassword] = useState(false);

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
            userSource: '',
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
            name="email"
            label={<T keyName="sign_up_form_email" />}
            autoComplete="email"
          />
          <PasswordFieldWithValidation
            label={<PasswordLabel />}
            inputProps={{ type: showPassword ? 'text' : 'password' }}
            InputProps={{
              endAdornment: (
                <Tooltip
                  title={
                    showPassword
                      ? t('sign_up_form_hide_password')
                      : t('sign_up_form_show_password')
                  }
                >
                  <IconButton
                    size="small"
                    onClick={() => setShowPassword((v) => !v)}
                    tabIndex={-1}
                  >
                    {showPassword ? <EyeOff /> : <Eye />}
                  </IconButton>
                </Tooltip>
              ),
            }}
          />
          <TextField
            autoComplete="name"
            name="name"
            label={<T keyName="sign_up_form_full_name" />}
          />
          {orgRequired && (
            <TextField
              autoComplete="organization"
              name="organizationName"
              label={<T keyName="sign_up_form_organization_name" />}
            />
          )}
          {userSourceField && (
            <TextField
              autoComplete="off"
              name="userSource"
              label={<T keyName="sign_up_form_user_source" />}
            />
          )}
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
