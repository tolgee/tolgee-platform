import { useEffect, useState } from 'react';
import {
  Box,
  Button,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Typography,
} from '@mui/material';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { T } from '@tolgee/react';

import { useApiMutation } from 'tg.service/http/useQueryApi';
import { components } from 'tg.service/apiSchema.generated';

import { AppSummary } from 'tg.component/apps/AppSummary';
import { AppChips } from 'tg.component/apps/AppChips';

type AppInstallModel = components['schemas']['AppInstallModel'];
type AppManifestPreviewModel = components['schemas']['AppManifestPreviewModel'];

type Props = {
  organizationId: number;
  install: AppInstallModel;
  onClose: () => void;
};

export const RefreshAppDialog = ({
  organizationId,
  install,
  onClose,
}: Props) => {
  const [preview, setPreview] = useState<AppManifestPreviewModel | null>(null);

  const previewMutation = useApiMutation({
    url: '/v2/organizations/{organizationId}/apps/preview',
    method: 'post',
  });

  const refreshMutation = useApiMutation({
    url: '/v2/organizations/{organizationId}/apps/{installId}/refresh',
    method: 'post',
    invalidatePrefix: '/v2/organizations/{organizationId}/apps',
  });

  useEffect(() => {
    previewMutation.mutate(
      {
        path: { organizationId },
        content: { 'application/json': { manifestUrl: install.manifestUrl } },
      },
      {
        onSuccess: (data) => setPreview(data),
      }
    );
  }, []);

  const handleConfirm = () => {
    refreshMutation.mutate(
      { path: { organizationId, installId: install.id } },
      { onSuccess: onClose }
    );
  };

  const currentScopes = new Set(install.scopes);
  const requested = preview?.requestedScopes ?? [];
  const requestedScopes = new Set(requested);
  const addedScopes = requested.filter((scope) => !currentScopes.has(scope));
  const keptScopes = requested.filter((scope) => currentScopes.has(scope));
  const removedScopes = install.scopes.filter(
    (scope) => !requestedScopes.has(scope)
  );

  return (
    <Dialog
      open
      onClose={onClose}
      maxWidth="sm"
      fullWidth
      data-cy="organization-apps-refresh-dialog"
    >
      <DialogTitle>
        <T
          keyName="organization_apps_refresh_dialog_title"
          defaultValue="Refresh app"
        />
      </DialogTitle>

      <DialogContent data-cy="organization-apps-refresh-content">
        {previewMutation.isLoading && (
          <Box display="flex" justifyContent="center" py={4}>
            <CircularProgress size={24} />
          </Box>
        )}

        {preview && (
          <>
            <Box mb={2}>
              <AppSummary
                name={preview.name}
                version={preview.version}
                url={preview.baseUrl}
              />
            </Box>

            {addedScopes.length > 0 && (
              <Box mb={2}>
                <Typography variant="body2" mb={1}>
                  <T
                    keyName="organization_apps_refresh_added_scopes"
                    defaultValue="New permissions requested:"
                  />
                </Typography>
                <AppChips
                  items={addedScopes}
                  color="warning"
                  dataCy="organization-apps-refresh-scope-added"
                />
              </Box>
            )}

            {removedScopes.length > 0 && (
              <Box mb={2}>
                <Typography variant="body2" mb={1}>
                  <T
                    keyName="organization_apps_refresh_removed_scopes"
                    defaultValue="Permissions no longer requested:"
                  />
                </Typography>
                <AppChips
                  items={removedScopes}
                  variant="outlined"
                  dataCy="organization-apps-refresh-scope-removed"
                />
              </Box>
            )}

            {keptScopes.length > 0 && (
              <Box mb={1}>
                <Typography variant="body2" mb={1}>
                  <T
                    keyName="organization_apps_refresh_kept_scopes"
                    defaultValue="Permissions already granted:"
                  />
                </Typography>
                <AppChips
                  items={keptScopes}
                  dataCy="organization-apps-refresh-scope-kept"
                />
              </Box>
            )}

            {requested.length === 0 && removedScopes.length === 0 && (
              <Typography variant="body2" color="text.secondary">
                <T
                  keyName="organization_apps_refresh_no_scopes"
                  defaultValue="This app does not request any permissions."
                />
              </Typography>
            )}
          </>
        )}
      </DialogContent>
      <DialogActions>
        <Button
          data-cy="organization-apps-refresh-cancel"
          onClick={onClose}
          disabled={refreshMutation.isLoading}
        >
          <T keyName="cancel_button" defaultValue="Cancel" />
        </Button>
        <LoadingButton
          data-cy="organization-apps-refresh-submit"
          variant="contained"
          color="primary"
          loading={refreshMutation.isLoading}
          disabled={!preview}
          onClick={handleConfirm}
        >
          <T
            keyName="organization_apps_refresh_submit"
            defaultValue="Approve & update"
          />
        </LoadingButton>
      </DialogActions>
    </Dialog>
  );
};
