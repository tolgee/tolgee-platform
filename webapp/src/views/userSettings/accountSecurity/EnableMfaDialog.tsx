import React, { FunctionComponent, useEffect, useMemo, useState } from 'react';
import ReactQR from 'react-qr-code';
import {
  Box,
  Dialog,
  DialogContent,
  DialogTitle,
  styled,
  Typography,
} from '@mui/material';
import { T, useTranslate } from '@tolgee/react';

import { useApiMutation } from 'tg.service/http/useQueryApi';
import { components } from 'tg.service/apiSchema.generated';
import { StandardForm } from 'tg.component/common/form/StandardForm';
import { LINKS } from 'tg.constants/links';
import { TextField } from 'tg.component/common/form/fields/TextField';
import { useMessage } from 'tg.hooks/useSuccessMessage';
import { useUser } from 'tg.globalContext/helpers';
import { useGlobalActions } from 'tg.globalContext/GlobalContext';
import { Validation } from 'tg.constants/GlobalValidationSchema';

import { MfaRecoveryCodesDialog } from './MfaRecoveryCodesDialog';
import { useHistory } from 'react-router-dom';

type TotpEnableDto = components['schemas']['UserTotpEnableRequestDto'];

const BASE32_ALPHABET = 'abcdefghijklmnopqrstuvwxyz234567';

const WhiteBox = styled(Box)`
  background-color: white;
  border-radius: 8px;
`;

export const EnableMfaDialog: FunctionComponent = () => {
  const [recoveryCodesPw, setRecoveryCodesPw] = useState<string | null>(null);
  const history = useHistory();

  const onDialogClose = () => {
    if (recoveryCodesPw) return;
    history.push(LINKS.USER_ACCOUNT_SECURITY.build());
  };

  const secret = useMemo(() => {
    // Generate 96 bits of secure random data in 32-bits chunks
    const secureRng = new Int32Array(3);
    window.crypto.getRandomValues(secureRng);

    let str = '';
    // Turn the random bits into a random Base32 string
    // We iterate over our random chunks
    for (let i = 0; i < 3; i++) {
      const v = secureRng[i];

      // log2(32) == 5 ==> we need to peek 5 bits of data to get 1 letter
      // shift & mask here we go
      str += BASE32_ALPHABET[(v >>> 27) & 0x1f];
      str += BASE32_ALPHABET[(v >>> 22) & 0x1f];
      str += BASE32_ALPHABET[(v >>> 17) & 0x1f];
      str += BASE32_ALPHABET[(v >>> 12) & 0x1f];
      str += BASE32_ALPHABET[(v >>> 7) & 0x1f];
      str += BASE32_ALPHABET[(v >>> 2) & 0x1f];
    }

    // We generated more than 16 chars, so we truncate the string
    return str.slice(0, 16);
  }, []);

  const user = useUser();
  const message = useMessage();
  const { t } = useTranslate();
  const { handleAfterLogin } = useGlobalActions();

  useEffect(() => {
    if (user && user.mfaEnabled) onDialogClose();
  }, [user]);

  const enableMfa = useApiMutation({
    url: '/v2/user/mfa/totp',
    method: 'put',
    options: {
      onSuccess: (r, v) => {
        handleAfterLogin(r);
        message.success(<T keyName="account-security-mfa-enabled-success" />);
        setRecoveryCodesPw(v.content['application/json'].password);
      },
    },
  });

  if (!user) return null;
  if (recoveryCodesPw) {
    return <MfaRecoveryCodesDialog password={recoveryCodesPw} />;
  }

  const encodedOtpName = encodeURIComponent(`Tolgee (${user.username})`);
  const otpUri = `otpauth://totp/${encodedOtpName}?secret=${secret}`;

  return (
    <Dialog
      open={true}
      onClose={onDialogClose}
      fullWidth
      maxWidth="md"
      data-cy="mfa-enable-dialog"
    >
      <DialogTitle data-cy="mfa-enable-dialog-title">
        <T keyName="account-security-mfa-enable-mfa" />
      </DialogTitle>
      <DialogContent data-cy="mfa-enable-dialog-content">
        <Box
          display="flex"
          flexDirection={{ xs: 'column', md: 'row' }}
          gap={{ xs: 2, md: 8 }}
        >
          <Box flex={0} flexShrink={0}>
            <Typography>
              <T keyName="account-security-mfa-enable-step-one" />
            </Typography>
            <Box display="flex" justifyContent="center" flex={1}>
              <WhiteBox p={2} mt={1} mb={1}>
                <ReactQR value={otpUri} size={196} />
              </WhiteBox>
            </Box>
            <Typography variant="body2" mb={0.5}>
              <T keyName="account-security-mfa-enable-manual-entry" />
            </Typography>
            <code data-cy="mfa-enable-dialog-totp-key">
              {secret.replace(/.{4}(?=.)/g, '$& ')}
            </code>
          </Box>
          <Box flex={1}>
            <Typography mb={2}>
              <T keyName="account-security-mfa-enable-step-two" />
            </Typography>
            <StandardForm
              onSubmit={(values) => {
                enableMfa.mutate({
                  content: { 'application/json': values },
                });
              }}
              saveActionLoadable={enableMfa}
              onCancel={() => onDialogClose()}
              initialValues={
                {
                  totpKey: secret,
                  password: '',
                  otp: '',
                } as TotpEnableDto
              }
              submitButtonInner={t('account-security-mfa-enable-mfa-button')}
              validationSchema={Validation.USER_MFA_ENABLE}
            >
              <TextField
                inputProps={{
                  'data-cy': 'mfa-enable-dialog-password-input',
                }}
                name="password"
                type="password"
                label={t('Password')}
              />
              <TextField
                inputProps={{
                  'data-cy': 'mfa-enable-dialog-otp-input',
                }}
                name="otp"
                placeholder="000000"
                label={t('account-security-mfa-otp-code')}
              />
            </StandardForm>
          </Box>
        </Box>
      </DialogContent>
    </Dialog>
  );
};
