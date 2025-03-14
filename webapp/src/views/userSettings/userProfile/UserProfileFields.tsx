import { useFormikContext } from 'formik';
import { Box, Grid, Typography } from '@mui/material';
import { UserProfileAvatar } from 'tg.views/userSettings/userProfile/UserProfileAvatar';
import { TextField } from 'tg.component/common/form/fields/TextField';
import { T, useTranslate } from '@tolgee/react';
import { useConfig, useUser } from 'tg.globalContext/helpers';
import { FunctionComponent } from 'react';

export const UserProfileFields: FunctionComponent = () => {
  const { t } = useTranslate();
  const config = useConfig();
  const user = useUser();
  const isManaged = user?.accountType === 'MANAGED';

  const formik = useFormikContext();
  const initialEmail = formik.getFieldMeta('email').initialValue;
  const newEmail = formik.getFieldMeta('email').value;
  const emailChanged = newEmail !== initialEmail;

  return (
    <Box data-cy="user-profile" sx={{ mb: 2 }}>
      <Grid container spacing={8}>
        <Grid item xs="auto">
          <UserProfileAvatar />
        </Grid>
        <Grid item xs={12} sm>
          <TextField
            name="name"
            label={<T keyName="User settings - Full name" />}
          />
          <TextField
            name="email"
            disabled={isManaged}
            helperText={isManaged ? t('managed-account-field-hint') : undefined}
            label={<T keyName="User settings - E-mail" />}
          />
          {user?.emailAwaitingVerification && (
            <Box>
              <Typography variant="body1">
                <T
                  keyName="email_waiting_for_verification"
                  params={{
                    email: user.emailAwaitingVerification!,
                  }}
                />
              </Typography>
            </Box>
          )}

          {emailChanged && config.needsEmailVerification && (
            <Typography variant="body1">
              <T keyName="your_email_was_changed_verification_message" />
            </Typography>
          )}
        </Grid>
      </Grid>

      {emailChanged && (
        <TextField
          name="currentPassword"
          type="password"
          label={<T keyName="current-password" />}
        />
      )}
    </Box>
  );
};
