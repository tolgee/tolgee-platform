import { useFormikContext } from 'formik';
import { Box, Grid, MenuItem, Typography } from '@mui/material';
import { UserProfileAvatar } from 'tg.views/userSettings/userProfile/UserProfileAvatar';
import { TextField } from 'tg.component/common/form/fields/TextField';
import { Select } from 'tg.component/common/form/fields/Select';
import { T, useTranslate } from '@tolgee/react';
import { useConfig, useUser } from 'tg.globalContext/helpers';
import { FunctionComponent } from 'react';

export const UserProfileFields: FunctionComponent<
  React.PropsWithChildren<unknown>
> = () => {
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
          <Select
            name="profileVisibility"
            data-cy="user-profile-visibility-select"
            label={t(
              'user_profile_visibility_label',
              'Community profile visibility'
            )}
          >
            <MenuItem value="DEFAULT">
              {t(
                'user_profile_visibility_default',
                'Default (public once you contribute to a community project)'
              )}
            </MenuItem>
            <MenuItem value="PUBLIC">
              {t('user_profile_visibility_public', 'Public')}
            </MenuItem>
            <MenuItem value="PRIVATE">
              {t('user_profile_visibility_private', 'Private')}
            </MenuItem>
          </Select>
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
