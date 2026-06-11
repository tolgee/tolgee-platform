import { useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  IconButton,
  TextField,
  Tooltip,
  Typography,
} from '@mui/material';
import { LoadingButton } from '@mui/lab';
import { Copy06 } from '@untitled-ui/icons-react';
import { T, useTranslate } from '@tolgee/react';

import { useOrganization } from 'tg.views/organizations/useOrganization';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { components } from 'tg.service/apiSchema.generated';

type AppManifestPreviewModel = components['schemas']['AppManifestPreviewModel'];
type AppRegistrationResponseModel =
  components['schemas']['AppRegistrationResponseModel'];

type Props = {
  open: boolean;
  onClose: () => void;
};

export const RegisterAppDialog = ({ open, onClose }: Props) => {
  const organization = useOrganization();
  const { t } = useTranslate();

  const [manifestUrl, setManifestUrl] = useState('');
  const [preview, setPreview] = useState<AppManifestPreviewModel | null>(null);
  const [registered, setRegistered] =
    useState<AppRegistrationResponseModel | null>(null);

  const previewMutation = useApiMutation({
    url: '/v2/organizations/{organizationId}/apps/preview',
    method: 'post',
  });

  const registerMutation = useApiMutation({
    url: '/v2/organizations/{organizationId}/apps',
    method: 'post',
    invalidatePrefix: '/v2/organizations/{organizationId}/apps',
  });

  const reset = () => {
    setManifestUrl('');
    setPreview(null);
    setRegistered(null);
    previewMutation.reset();
    registerMutation.reset();
  };

  const handleClose = () => {
    reset();
    onClose();
  };

  const handlePreview = (event: React.FormEvent) => {
    event.preventDefault();
    if (!organization || !manifestUrl) return;
    previewMutation.mutate(
      {
        path: { organizationId: organization.id },
        content: { 'application/json': { manifestUrl } },
      },
      {
        onSuccess: (data) => setPreview(data),
      }
    );
  };

  const handleInstall = () => {
    if (!organization || !manifestUrl) return;
    registerMutation.mutate(
      {
        path: { organizationId: organization.id },
        content: { 'application/json': { manifestUrl } },
      },
      { onSuccess: (data) => setRegistered(data) }
    );
  };

  const step: 'url' | 'consent' | 'reveal' = registered
    ? 'reveal'
    : preview
    ? 'consent'
    : 'url';

  return (
    <Dialog
      open={open}
      onClose={step === 'reveal' ? undefined : handleClose}
      maxWidth="sm"
      fullWidth
      data-cy="organization-apps-register-dialog"
    >
      <DialogTitle>
        {step === 'reveal' ? (
          <T
            keyName="organization_apps_register_secrets_title"
            defaultValue="Copy the credentials now"
          />
        ) : (
          <T
            keyName="organization_apps_register_dialog_title"
            defaultValue="Register app"
          />
        )}
      </DialogTitle>

      {step === 'url' && (
        <form onSubmit={handlePreview}>
          <DialogContent>
            <TextField
              data-cy="organization-apps-register-manifest-url"
              label={t(
                'organization_apps_register_manifest_url_label',
                'Manifest URL'
              )}
              helperText={t(
                'organization_apps_register_manifest_url_helper',
                'URL to the app manifest.json file.'
              )}
              value={manifestUrl}
              onChange={(event) => setManifestUrl(event.target.value)}
              fullWidth
              autoFocus
              margin="normal"
              placeholder="https://my-plugin.example.com/manifest.json"
            />
          </DialogContent>
          <DialogActions>
            <Button onClick={handleClose}>
              <T keyName="cancel_button" defaultValue="Cancel" />
            </Button>
            <LoadingButton
              data-cy="organization-apps-register-continue"
              type="submit"
              variant="contained"
              color="primary"
              loading={previewMutation.isLoading}
              disabled={!manifestUrl}
            >
              <T
                keyName="organization_apps_register_continue"
                defaultValue="Continue"
              />
            </LoadingButton>
          </DialogActions>
        </form>
      )}

      {step === 'consent' && preview && (
        <>
          <DialogContent data-cy="organization-apps-register-consent">
            <Box mb={2}>
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
              <Typography variant="body2" color="text.secondary">
                {preview.baseUrl}
              </Typography>
            </Box>

            <Typography variant="body2" mb={1}>
              <T
                keyName="organization_apps_register_consent_intro"
                defaultValue="This app requests the following permissions:"
              />
            </Typography>

            {preview.requestedScopes.length === 0 && (
              <Typography
                variant="body2"
                color="text.secondary"
                data-cy="organization-apps-register-consent-no-scopes"
              >
                <T
                  keyName="organization_apps_register_consent_no_scopes"
                  defaultValue="No scopes requested."
                />
              </Typography>
            )}

            <Box display="flex" flexWrap="wrap" gap={1}>
              {preview.requestedScopes.map((scope) => (
                <Chip
                  key={scope}
                  size="small"
                  label={scope}
                  data-cy="organization-apps-register-consent-scope"
                />
              ))}
            </Box>

            {preview.requestedWebhookEvents.length > 0 && (
              <Box mt={3}>
                <Typography variant="body2" mb={1}>
                  <T
                    keyName="organization_apps_register_consent_webhook_intro"
                    defaultValue="And will receive webhooks for these events:"
                  />
                </Typography>
                <Box display="flex" flexWrap="wrap" gap={1}>
                  {preview.requestedWebhookEvents.map((event) => (
                    <Chip
                      key={event}
                      size="small"
                      color="info"
                      label={event}
                      data-cy="organization-apps-register-consent-webhook"
                    />
                  ))}
                </Box>
              </Box>
            )}
          </DialogContent>
          <DialogActions>
            <Button
              data-cy="organization-apps-register-back"
              onClick={() => setPreview(null)}
              disabled={registerMutation.isLoading}
            >
              <T
                keyName="organization_apps_register_back"
                defaultValue="Back"
              />
            </Button>
            <LoadingButton
              data-cy="organization-apps-register-submit"
              variant="contained"
              color="primary"
              loading={registerMutation.isLoading}
              onClick={handleInstall}
            >
              <T
                keyName="organization_apps_register_submit"
                defaultValue="Approve & install"
              />
            </LoadingButton>
          </DialogActions>
        </>
      )}

      {step === 'reveal' && registered && (
        <>
          <DialogContent data-cy="organization-apps-register-reveal">
            <Alert severity="warning" sx={{ mb: 2 }}>
              <T
                keyName="organization_apps_register_secrets_warning"
                defaultValue="The client secret is shown only once. Copy it into the plugin's environment now — you cannot retrieve it later."
              />
            </Alert>
            <CopyableSecret
              label={t('organization_apps_register_client_id', 'Client ID')}
              value={registered.clientId ?? ''}
              dataCy="organization-apps-register-secret-client-id"
            />
            <CopyableSecret
              label={t(
                'organization_apps_register_client_secret',
                'Client secret'
              )}
              value={registered.clientSecret}
              dataCy="organization-apps-register-secret-client-secret"
            />
            <CopyableSecret
              label={t(
                'organization_apps_register_webhook_secret',
                'Webhook secret'
              )}
              value={registered.webhookSecret ?? ''}
              dataCy="organization-apps-register-secret-webhook-secret"
            />
          </DialogContent>
          <DialogActions>
            <Button
              data-cy="organization-apps-register-done"
              variant="contained"
              color="primary"
              onClick={handleClose}
            >
              <T
                keyName="organization_apps_register_done"
                defaultValue="I've copied the credentials"
              />
            </Button>
          </DialogActions>
        </>
      )}
    </Dialog>
  );
};

const CopyableSecret = ({
  label,
  value,
  dataCy,
}: {
  label: string;
  value: string;
  dataCy: string;
}) => {
  const { t } = useTranslate();
  return (
    <Box mb={2}>
      <Typography variant="caption" color="text.secondary">
        {label}
      </Typography>
      <Box display="flex" alignItems="center" gap={1}>
        <Box
          flex={1}
          sx={{
            fontFamily: 'monospace',
            fontSize: '0.85rem',
            background: (theme) => theme.palette.action.hover,
            borderRadius: 1,
            padding: '6px 10px',
            wordBreak: 'break-all',
          }}
          data-cy={dataCy}
        >
          {value || '—'}
        </Box>
        {value && (
          <Tooltip
            title={t(
              'organization_apps_register_secret_copy_tooltip',
              'Copy to clipboard'
            )}
          >
            <IconButton
              size="small"
              onClick={() => navigator.clipboard?.writeText(value)}
              data-cy={`${dataCy}-copy`}
            >
              <Copy06 width={16} height={16} />
            </IconButton>
          </Tooltip>
        )}
      </Box>
    </Box>
  );
};
