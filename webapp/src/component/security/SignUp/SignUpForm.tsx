import React, { useState } from 'react';
import {
  Box,
  IconButton,
  Link,
  styled,
  Tooltip,
  Typography,
} from '@mui/material';
import { LoadingButton } from '@mui/lab';
import { T, useTranslate } from '@tolgee/react';
import { Eye, EyeOff } from '@untitled-ui/icons-react';

import {
  LoadableType,
  StandardForm,
} from 'tg.component/common/form/StandardForm';
import { TextField } from 'tg.component/common/form/fields/TextField';
import { Validation } from 'tg.constants/GlobalValidationSchema';
import { useConfig } from 'tg.globalContext/helpers';
import { PasswordLabel } from '../SetPasswordField';
import { LabelHint } from 'tg.component/common/LabelHint';
import { useGlobalContext } from 'tg.globalContext/GlobalContext';
import { SignUpError } from './SignUpError';

const StyledInputFields = styled('div')`
  display: grid;
  align-items: start;
  padding-bottom: 12px;
`;

const StyledSeparator = styled('div')`
  height: 32px;
  justify-self: center;
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

export const SignUpForm = (props: Props) => {
  const invitationCode = useGlobalContext((c) => c.auth.invitationCode);
  const config = useConfig();
  const orgRequired = !invitationCode && config.userCanCreateOrganizations;
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
          <SignUpError loadable={props.loadable} />

          <TextField
            autoComplete="name"
            name="name"
            label={<T keyName="sign_up_form_full_name" />}
          />
          {orgRequired && (
            <TextField
              autoComplete="organization"
              name="organizationName"
              label={
                <LabelHint title={t('sign_up_form_organization_hint')}>
                  <T keyName="sign_up_form_organization_name" />
                </LabelHint>
              }
            />
          )}
          {userSourceField && (
            <TextField
              autoComplete="off"
              name="userSource"
              label={
                <>
                  <T keyName="sign_up_form_user_source" /> (
                  <T keyName="sign_up_form_optional_label" />)
                </>
              }
            />
          )}

          <StyledSeparator />

          <TextField
            name="email"
            label={<T keyName="sign_up_form_email" />}
            autoComplete="username email"
          />
          <PasswordFieldWithValidation
            label={<PasswordLabel />}
            inputProps={{ type: showPassword ? 'text' : 'password' }}
            autoComplete="password"
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
          <Box mt={1}>
            <Typography variant="caption" fontSize={14}>
              <T
                keyName="sign-up-terms-and-conditions-message"
                params={{
                  Link: <Link href="https://tolgee.io/terms_and_conditions" />,
                }}
              />
            </Typography>
          </Box>
        </StyledInputFields>
      </StandardForm>
    </>
  );
};
