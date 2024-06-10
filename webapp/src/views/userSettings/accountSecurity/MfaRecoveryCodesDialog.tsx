import { FunctionComponent, useEffect, useState } from 'react';
import {
  Box,
  Dialog,
  DialogContent,
  DialogTitle,
  Grid,
  Typography,
} from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { StandardForm } from 'tg.component/common/form/StandardForm';
import { LINKS } from 'tg.constants/links';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { components } from 'tg.service/apiSchema.generated';
import { TextField } from 'tg.component/common/form/fields/TextField';
import { useUser } from 'tg.globalContext/helpers';
import { BoxLoading } from 'tg.component/common/BoxLoading';
import DialogActions from '@mui/material/DialogActions';
import Button from '@mui/material/Button';
import { Validation } from 'tg.constants/GlobalValidationSchema';
import { useHistory } from 'react-router-dom';

type MfaRecoveryDto = components['schemas']['UserMfaRecoveryRequestDto'];

type MfaRecoveryCodesDialogProps = {
  // This is used by EnableMfaDialog to automatically generate recovery codes without the user re-entering credentials.
  password?: string;
};

export const MfaRecoveryCodesDialog: FunctionComponent<
  MfaRecoveryCodesDialogProps
> = ({ password: providedPassword }) => {
  const [insertedPassword, setInsertedPassword] = useState<string>();
  const history = useHistory();
  const onDialogClose = () => {
    history.push(LINKS.USER_ACCOUNT_SECURITY.build());
    setInsertedPassword(undefined);
  };

  const user = useUser();
  const { t } = useTranslate();

  useEffect(() => {
    if (!providedPassword && user && !user.mfaEnabled) onDialogClose();
  }, [user, providedPassword]);

  const password = insertedPassword || providedPassword;

  const codesLoadable = useApiQuery({
    url: '/v2/user/mfa/recovery',
    method: 'put',
    options: {
      enabled: Boolean(password),
      staleTime: 0,
      cacheTime: 0,
    },
    content: {
      'application/json': { password: password! },
    },
  });

  useEffect(() => {
    if (password) {
      codesLoadable.refetch();
    }
  }, [password]);

  if (!user) return null;

  return (
    <Dialog
      open={true}
      onClose={onDialogClose}
      fullWidth
      maxWidth="sm"
      data-cy="mfa-recovery-codes-dialog"
    >
      <DialogTitle data-cy="mfa-recovery-codes-dialog-title">
        <T keyName="account-security-mfa-recovery-codes" />
      </DialogTitle>
      <DialogContent data-cy="mfa-recovery-codes-dialog-content">
        {codesLoadable.isLoading ? (
          <BoxLoading />
        ) : codesLoadable.data ? (
          <Box>
            <Typography mb={2}>
              <T keyName="account-security-mfa-recovery-codes-description" />
            </Typography>
            <Grid container spacing={2} component="ul">
              {codesLoadable.data.map((code) => (
                <Grid item xs={6} key={code} component="li">
                  <code>{code}</code>
                </Grid>
              ))}
            </Grid>
          </Box>
        ) : (
          <StandardForm
            onSubmit={(values) => {
              setInsertedPassword(values.password);
            }}
            onCancel={() => onDialogClose()}
            initialValues={
              {
                password: '',
              } as MfaRecoveryDto
            }
            submitButtonInner={t('account-security-mfa-view-recovery')}
            validationSchema={Validation.USER_MFA_VIEW_RECOVERY}
          >
            <Typography>
              <T
                keyName="account-security-mfa-recovery-info"
                params={{ b: <b /> }}
              />
            </Typography>
            <TextField
              inputProps={{
                'data-cy': 'mfa-recovery-codes-dialog-password-input',
              }}
              name="password"
              type="password"
              label={t('Password')}
            />
            <Typography variant="body2" mb={2}>
              <T keyName="account-security-mfa-recovery-info-invalidate" />
            </Typography>
          </StandardForm>
        )}
      </DialogContent>
      {codesLoadable.data && (
        <DialogActions>
          <Button
            data-cy="mfa-recovery-codes-dialog-close"
            onClick={() => onDialogClose()}
            type="button"
          >
            <T keyName="global_close_button" />
          </Button>
        </DialogActions>
      )}
    </Dialog>
  );
};
