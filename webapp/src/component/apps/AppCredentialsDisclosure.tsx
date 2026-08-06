import React from 'react';
import { Alert, Box, Typography } from '@mui/material';
import { T } from '@tolgee/react';

import { ClipboardCopyInput } from 'tg.component/common/ClipboardCopyInput';

type Props = {
  clientId?: string;
  clientSecret?: string;
  webhookSecret?: string;
  dataCy: string;
};

const Field = ({
  label,
  value,
  dataCy,
}: {
  label: React.ReactNode;
  value: string;
  dataCy: string;
}) => (
  <Box mb={1.5}>
    <Typography variant="caption" color="text.secondary">
      {label}
    </Typography>
    <Box data-cy={dataCy} data-sentry-mask="">
      <ClipboardCopyInput value={value} />
    </Box>
  </Box>
);

/**
 * The app-level credentials, which Tolgee stores hashed and can therefore never show again.
 * Whoever registered the app is the only one who ever sees them.
 */
export const AppCredentialsDisclosure = ({
  clientId,
  clientSecret,
  webhookSecret,
  dataCy,
}: Props) => (
  <Box data-cy={dataCy}>
    <Alert severity="warning" sx={{ mb: 2 }}>
      <T
        keyName="app_credentials_shown_once_warning"
        defaultValue="Copy these now — they are shown only once and cannot be retrieved again. They administer the app across every organization that installs it and grant access to no translation data."
      />
    </Alert>

    {clientId && (
      <Field
        label={
          <T
            keyName="app_credentials_client_id_label"
            defaultValue="Client ID"
          />
        }
        value={clientId}
        dataCy="app-credentials-client-id"
      />
    )}

    {clientSecret && (
      <Field
        label={
          <T
            keyName="app_credentials_client_secret_label"
            defaultValue="Client secret"
          />
        }
        value={clientSecret}
        dataCy="app-credentials-client-secret"
      />
    )}

    {webhookSecret && (
      <Field
        label={
          <T
            keyName="app_credentials_webhook_secret_label"
            defaultValue="Webhook signing secret"
          />
        }
        value={webhookSecret}
        dataCy="app-credentials-webhook-secret"
      />
    )}
  </Box>
);
