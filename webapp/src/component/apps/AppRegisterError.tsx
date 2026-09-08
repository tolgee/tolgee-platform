import { Alert, Box } from '@mui/material';
import { T } from '@tolgee/react';

import { ApiError } from 'tg.service/http/ApiError';

type Props = {
  error: ApiError | null | undefined;
  dataCy?: string;
};

/**
 * Renders a preview/register failure inline in the register dialog: a plain-language explanation
 * plus the next step, instead of the raw server error or code. For `app_manifest_invalid` the
 * server also returns the individual manifest problems, listed underneath.
 */
export const AppRegisterError = ({ error, dataCy }: Props) => {
  if (!error) {
    return null;
  }

  const code = error.code ?? '';
  const details = (error.params ?? []).filter(
    (p): p is string => typeof p === 'string'
  );

  return (
    <Alert
      severity="error"
      sx={{ mb: 2 }}
      data-cy={dataCy ?? 'app-register-error'}
    >
      <AppRegisterErrorMessage code={code} />
      {code === 'app_manifest_invalid' && details.length > 0 && (
        <Box component="ul" sx={{ mt: 1, mb: 0, pl: 3 }}>
          {details.map((detail, index) => (
            <li key={index}>{detail}</li>
          ))}
        </Box>
      )}
    </Alert>
  );
};

const AppRegisterErrorMessage = ({ code }: { code: string }) => {
  if (code === 'app_manifest_same_origin_as_tolgee') {
    return (
      <T
        keyName="app_register_error_same_origin"
        defaultValue="This app's base URL points back at your Tolgee instance, which isn't allowed. Host the app on a different domain or port, then register it again."
      />
    );
  }

  if (code === 'app_manifest_invalid') {
    return (
      <T
        keyName="app_register_error_manifest_invalid"
        defaultValue="Tolgee can't accept this app manifest. Ask the app's author to fix the problems below (or fix the manifest yourself), then try again:"
      />
    );
  }

  if (code === 'app_manifest_fetch_failed') {
    return (
      <T
        keyName="app_register_error_fetch_failed"
        defaultValue="Tolgee couldn't fetch a manifest from that URL. Check the URL is correct and reachable, and that it returns the app's manifest.json."
      />
    );
  }

  if (code === 'app_already_registered') {
    return (
      <T
        keyName="app_register_error_already_registered"
        defaultValue="This app is already registered on this server. Open it from the Owned apps list to manage its credentials."
      />
    );
  }

  return (
    <T
      keyName="app_register_error_generic"
      defaultValue="Something went wrong while registering the app. Check the manifest URL and try again."
    />
  );
};
