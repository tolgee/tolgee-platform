import { useState } from 'react';
import {
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  TextField,
} from '@mui/material';
import { LoadingButton } from '@mui/lab';
import { T, useTranslate } from '@tolgee/react';

import { useOrganization } from 'tg.views/organizations/useOrganization';
import { useApiMutation } from 'tg.service/http/useQueryApi';

type Props = {
  open: boolean;
  onClose: () => void;
};

export const RegisterAppDialog = ({ open, onClose }: Props) => {
  const organization = useOrganization();
  const { t } = useTranslate();
  const [manifestUrl, setManifestUrl] = useState('');

  const registerMutation = useApiMutation({
    url: '/v2/organizations/{organizationId}/apps',
    method: 'post',
    invalidatePrefix: '/v2/organizations/{organizationId}/apps',
  });

  const handleClose = () => {
    setManifestUrl('');
    registerMutation.reset();
    onClose();
  };

  const handleSubmit = (event: React.FormEvent) => {
    event.preventDefault();
    if (!organization || !manifestUrl) return;
    registerMutation.mutate(
      {
        path: { organizationId: organization.id },
        content: { 'application/json': { manifestUrl } },
      },
      {
        onSuccess: handleClose,
      }
    );
  };

  return (
    <Dialog
      open={open}
      onClose={handleClose}
      maxWidth="sm"
      fullWidth
      data-cy="organization-apps-register-dialog"
    >
      <form onSubmit={handleSubmit}>
        <DialogTitle>
          <T
            keyName="organization_apps_register_dialog_title"
            defaultValue="Register app"
          />
        </DialogTitle>
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
            data-cy="organization-apps-register-submit"
            type="submit"
            variant="contained"
            color="primary"
            loading={registerMutation.isLoading}
            disabled={!manifestUrl}
          >
            <T
              keyName="organization_apps_register_submit"
              defaultValue="Register"
            />
          </LoadingButton>
        </DialogActions>
      </form>
    </Dialog>
  );
};
