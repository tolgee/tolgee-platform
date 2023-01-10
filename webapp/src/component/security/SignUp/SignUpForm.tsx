import { useSelector } from 'react-redux';
import { container } from 'tsyringe';
import { Box, Typography, Link } from '@mui/material';
import { LoadingButton } from '@mui/lab';
import { T, useTranslate } from '@tolgee/react';

import { StandardForm } from 'tg.component/common/form/StandardForm';
import { TextField } from 'tg.component/common/form/fields/TextField';
import { InvitationCodeService } from 'tg.service/InvitationCodeService';
import { AppState } from 'tg.store/index';
import { Validation } from 'tg.constants/GlobalValidationSchema';
import { SetPasswordFields } from '../SetPasswordFields';

const invitationService = container.resolve(InvitationCodeService);

export type SignUpType = {
  name: string;
  email: string;
  password: string;
  passwordRepeat?: string;
  organizationName: string;
  invitationCode?: string;
};

type Props = {
  onSubmit: (v) => void;
};

export const SignUpForm = (props: Props) => {
  const orgRequired = !invitationService.getCode();
  const state = useSelector((state: AppState) => state.signUp.loadables.signUp);
  const { t } = useTranslate();

  return (
    <StandardForm
      rootSx={{ mb: 1 }}
      saveActionLoadable={state}
      initialValues={
        {
          password: '',
          passwordRepeat: '',
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
            loading={state.loading}
          >
            <T>sign_up_submit_button</T>
          </LoadingButton>
        </Box>
      }
      onSubmit={props.onSubmit}
    >
      <TextField
        name="name"
        label={<T>sign_up_form_full_name</T>}
        variant="standard"
      />
      <TextField
        name="email"
        label={<T>sign_up_form_email</T>}
        variant="standard"
      />
      {orgRequired && (
        <TextField
          name="organizationName"
          label={<T>sign_up_form_organization_name</T>}
          variant="standard"
        />
      )}
      <SetPasswordFields />
      <Box mt={2} mb={3}>
        <Typography variant="body2">
          <T
            params={{
              Link(content) {
                return (
                  <Link href="https://tolgee.io/docs/terms_of_use">
                    {content}
                  </Link>
                );
              },
            }}
            keyName="sign-up-terms-and-conditions-message"
          />
        </Typography>
      </Box>
    </StandardForm>
  );
};
