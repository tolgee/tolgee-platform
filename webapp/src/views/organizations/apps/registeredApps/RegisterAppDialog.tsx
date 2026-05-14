import { useState } from 'react';
import {
  Box,
  Button,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  TextField,
  Typography,
} from '@mui/material';
import { LoadingButton } from '@mui/lab';
import { T, useTranslate } from '@tolgee/react';

import { useOrganization } from 'tg.views/organizations/useOrganization';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { components } from 'tg.service/apiSchema.generated';

type AppManifestPreviewModel = components['schemas']['AppManifestPreviewModel'];

type Props = {
  open: boolean;
  onClose: () => void;
};

export const RegisterAppDialog = ({ open, onClose }: Props) => {
  const organization = useOrganization();
  const { t } = useTranslate();

  const [manifestUrl, setManifestUrl] = useState('');
  const [preview, setPreview] = useState<AppManifestPreviewModel | null>(null);

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
      { onSuccess: handleClose }
    );
  };

  const step: 'url' | 'consent' = preview ? 'consent' : 'url';

  return (
    <Dialog
      open={open}
      onClose={handleClose}
      maxWidth="sm"
      fullWidth
      data-cy="organization-apps-register-dialog"
    >
      <DialogTitle>
        <T
          keyName="organization_apps_register_dialog_title"
          defaultValue="Register app"
        />
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
    </Dialog>
  );
};
