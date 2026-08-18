import React from 'react';
import { Alert, Box, Typography } from '@mui/material';
import { T } from '@tolgee/react';

import { ClipboardCopyInput } from 'tg.component/common/ClipboardCopyInput';

type DeliveryOutcome = {
  attempted: boolean;
  delivered: boolean;
  error?: string;
};

type Props = {
  clientId?: string;
  clientSecret?: string;
  webhookSecret?: string;
  /** Whether Tolgee managed to push these credentials to the app's base URL. */
  delivery?: DeliveryOutcome;
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
  delivery,
  dataCy,
}: Props) => (
  <Box data-cy={dataCy}>
    {delivery?.delivered ? (
      <Alert
        severity="success"
        sx={{ mb: 2 }}
        data-cy="app-credentials-delivered"
      >
        <T
          keyName="app_credentials_delivered"
          defaultValue="The app received these automatically — you don't have to copy anything. They are shown here once in case you keep your own record."
        />
      </Alert>
    ) : delivery?.attempted ? (
      <Alert
        severity="warning"
        sx={{ mb: 2 }}
        data-cy="app-credentials-delivery-failed"
      >
        <T
          keyName="app_credentials_delivery_failed"
          defaultValue="Tolgee couldn't hand these to the app ({error}). Copy them now and give them to the app by hand, or rotate later once the app is reachable. They are shown only once."
          params={{ error: delivery.error ?? '' }}
        />
      </Alert>
    ) : (
      <Alert
        severity="warning"
        sx={{ mb: 2 }}
        data-cy="app-credentials-copy-now"
      >
        <T
          keyName="app_credentials_shown_once_warning"
          defaultValue="Copy these now — they are shown only once and cannot be retrieved again. Store them like a password."
        />
      </Alert>
    )}

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
