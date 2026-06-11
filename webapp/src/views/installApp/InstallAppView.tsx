import { useEffect, useMemo, useState } from 'react';
import { useLocation } from 'react-router-dom';
import {
  Alert,
  Box,
  Button,
  Chip,
  Container,
  FormControl,
  InputLabel,
  MenuItem,
  Paper,
  Select,
  Typography,
} from '@mui/material';
import { LoadingButton } from '@mui/lab';
import { T, useTranslate } from '@tolgee/react';

import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import { components } from 'tg.service/apiSchema.generated';

type AppManifestPreviewModel = components['schemas']['AppManifestPreviewModel'];
type AppRegistrationResponseModel =
  components['schemas']['AppRegistrationResponseModel'];

const LOCALHOST_CALLBACK_RE =
  /^http:\/\/(127\.0\.0\.1|localhost)(:\d+)?(\/[^\s]*)?$/;

/**
 * Browser entry point for the CLI's `npm run register` flow. The CLI opens
 * the user's default browser at this URL with three query params:
 *   `manifestUrl`  — what to install
 *   `callback`     — http(s) URL the CLI's one-shot HTTP server is listening on
 *   `state`        — opaque CSRF token; echoed back verbatim
 *
 * Flow: pick org → preview → install → redirect to `callback` with the install
 * credentials in the query string. The callback URL is locked to localhost so
 * a malicious manifest can't trick the user into leaking `clientSecret` to a
 * remote host.
 */
export const InstallAppView = () => {
  const { t } = useTranslate();
  const location = useLocation();
  const params = useMemo(
    () => new URLSearchParams(location.search),
    [location.search]
  );
  const manifestUrl = params.get('manifestUrl') ?? '';
  const callback = params.get('callback') ?? '';
  const state = params.get('state') ?? '';
  const callbackValid = LOCALHOST_CALLBACK_RE.test(callback);

  const [organizationId, setOrganizationId] = useState<number | null>(null);
  const [preview, setPreview] = useState<AppManifestPreviewModel | null>(null);

  const organizations = useApiQuery({
    url: '/v2/organizations',
    method: 'get',
    // Register requires OWNER role; filter the list to only show orgs the
    // user can actually install into.
    query: { size: 100, sort: ['name'], filterCurrentUserOwner: true },
  });
  const orgItems = organizations.data?._embedded?.organizations ?? [];

  // Auto-select the only org so a single-org user goes straight to preview.
  useEffect(() => {
    if (organizationId == null && orgItems.length === 1) {
      setOrganizationId(orgItems[0].id);
    }
  }, [orgItems, organizationId]);

  const previewMutation = useApiMutation({
    url: '/v2/organizations/{organizationId}/apps/preview',
    method: 'post',
  });

  const registerMutation = useApiMutation({
    url: '/v2/organizations/{organizationId}/apps',
    method: 'post',
    invalidatePrefix: '/v2/organizations/{organizationId}/apps',
  });

  // Don't proceed if any of the required inputs are missing or the callback
  // is off-localhost — bail loudly before fetching anything.
  if (!manifestUrl || !callback || !state || !callbackValid) {
    return (
      <Container maxWidth="sm" sx={{ py: 6 }}>
        <Alert severity="error" data-cy="install-app-bad-params">
          {!manifestUrl || !callback || !state ? (
            <T
              keyName="install_app_missing_params"
              defaultValue="Missing required parameters in the install URL. Re-run `npm run register` from your plugin's project."
            />
          ) : (
            <T
              keyName="install_app_callback_not_local"
              defaultValue="The callback URL must be a localhost address. Refusing to redirect to {url}."
              params={{ url: callback }}
            />
          )}
        </Alert>
      </Container>
    );
  }

  const redirectToCallback = (qs: Record<string, string>) => {
    const url = new URL(callback);
    url.searchParams.set('state', state);
    for (const [k, v] of Object.entries(qs)) url.searchParams.set(k, v);
    window.location.replace(url.toString());
  };

  const handlePreview = () => {
    if (!organizationId) return;
    previewMutation.mutate(
      {
        path: { organizationId },
        content: { 'application/json': { manifestUrl } },
      },
      { onSuccess: (data) => setPreview(data) }
    );
  };

  const handleInstall = () => {
    if (!organizationId) return;
    registerMutation.mutate(
      {
        path: { organizationId },
        content: { 'application/json': { manifestUrl } },
      },
      {
        onSuccess: (data: AppRegistrationResponseModel) => {
          redirectToCallback({
            installId: String(data.id),
            organizationId: String(organizationId),
            clientId: data.clientId ?? '',
            clientSecret: data.clientSecret,
            webhookSecret: data.webhookSecret ?? '',
          });
        },
      }
    );
  };

  const handleCancel = () => {
    redirectToCallback({ error: 'cancelled' });
  };

  return (
    <Container maxWidth="sm" sx={{ py: 6 }}>
      <Paper sx={{ p: 4 }} data-cy="install-app-view">
        <Typography variant="h5" gutterBottom>
          <T
            keyName="install_app_title"
            defaultValue="Install a Tolgee plugin"
          />
        </Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
          <T
            keyName="install_app_subtitle"
            defaultValue="A plugin running on your machine has asked to install itself into your Tolgee."
          />
        </Typography>

        <Box mb={2}>
          <Typography variant="caption" color="text.secondary">
            <T
              keyName="install_app_manifest_url_label"
              defaultValue="Manifest URL"
            />
          </Typography>
          <Typography
            variant="body2"
            sx={{ fontFamily: 'monospace', wordBreak: 'break-all' }}
            data-cy="install-app-manifest-url"
          >
            {manifestUrl}
          </Typography>
        </Box>

        {!preview && (
          <>
            <FormControl fullWidth margin="normal">
              <InputLabel id="install-app-org-label">
                {t('install_app_org_label', 'Organization')}
              </InputLabel>
              <Select
                labelId="install-app-org-label"
                value={organizationId ?? ''}
                label={t('install_app_org_label', 'Organization')}
                onChange={(e) =>
                  setOrganizationId(Number(e.target.value) || null)
                }
                data-cy="install-app-org-select"
              >
                {orgItems.map((org) => (
                  <MenuItem key={org.id} value={org.id}>
                    {org.name}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            <Box display="flex" justifyContent="flex-end" gap={1} mt={3}>
              <Button onClick={handleCancel}>
                <T keyName="install_app_cancel" defaultValue="Cancel" />
              </Button>
              <LoadingButton
                variant="contained"
                color="primary"
                disabled={!organizationId}
                loading={previewMutation.isLoading}
                onClick={handlePreview}
                data-cy="install-app-continue"
              >
                <T keyName="install_app_continue" defaultValue="Continue" />
              </LoadingButton>
            </Box>
            {previewMutation.error && (
              <Alert severity="error" sx={{ mt: 2 }}>
                {previewMutation.error.message ??
                  JSON.stringify(previewMutation.error)}
              </Alert>
            )}
          </>
        )}

        {preview && (
          <Box mt={2} data-cy="install-app-consent">
            <Typography variant="subtitle1">
              {preview.name}{' '}
              <Typography
                component="span"
                variant="body2"
                color="text.secondary"
              >
                v{preview.version}
              </Typography>
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
              {preview.baseUrl}
            </Typography>

            <Typography variant="body2" mb={1}>
              <T
                keyName="install_app_scopes_intro"
                defaultValue="This plugin requests the following permissions:"
              />
            </Typography>
            {preview.requestedScopes.length === 0 ? (
              <Typography variant="body2" color="text.secondary">
                <T
                  keyName="install_app_no_scopes"
                  defaultValue="No scopes requested."
                />
              </Typography>
            ) : (
              <Box display="flex" flexWrap="wrap" gap={1}>
                {preview.requestedScopes.map((scope) => (
                  <Chip key={scope} size="small" label={scope} />
                ))}
              </Box>
            )}

            {preview.requestedWebhookEvents.length > 0 && (
              <Box mt={3}>
                <Typography variant="body2" mb={1}>
                  <T
                    keyName="install_app_webhooks_intro"
                    defaultValue="And will receive webhooks for these events:"
                  />
                </Typography>
                <Box display="flex" flexWrap="wrap" gap={1}>
                  {preview.requestedWebhookEvents.map((event) => (
                    <Chip key={event} size="small" color="info" label={event} />
                  ))}
                </Box>
              </Box>
            )}

            <Box display="flex" justifyContent="flex-end" gap={1} mt={4}>
              <Button
                onClick={() => setPreview(null)}
                disabled={registerMutation.isLoading}
              >
                <T keyName="install_app_back" defaultValue="Back" />
              </Button>
              <Button onClick={handleCancel}>
                <T keyName="install_app_cancel" defaultValue="Cancel" />
              </Button>
              <LoadingButton
                variant="contained"
                color="primary"
                loading={registerMutation.isLoading}
                onClick={handleInstall}
                data-cy="install-app-install"
              >
                <T
                  keyName="install_app_install"
                  defaultValue="Approve & install"
                />
              </LoadingButton>
            </Box>
            {registerMutation.error && (
              <Alert severity="error" sx={{ mt: 2 }}>
                {registerMutation.error.message ??
                  JSON.stringify(registerMutation.error)}
              </Alert>
            )}
          </Box>
        )}
      </Paper>
    </Container>
  );
};
