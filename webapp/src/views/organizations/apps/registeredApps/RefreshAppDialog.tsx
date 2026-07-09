import { useEffect, useState } from 'react';
import {
  Box,
  Button,
  Chip,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Typography,
} from '@mui/material';
import { LoadingButton } from '@mui/lab';
import { T } from '@tolgee/react';

import { useApiMutation } from 'tg.service/http/useQueryApi';
import { components } from 'tg.service/apiSchema.generated';

type AppInstallModel = components['schemas']['AppInstallModel'];
type AppManifestPreviewModel = components['schemas']['AppManifestPreviewModel'];

type Props = {
  open: boolean;
  organizationId: number;
  install: AppInstallModel;
  onClose: () => void;
};

export const RefreshAppDialog = ({
  open,
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
    if (!open) {
      setPreview(null);
      previewMutation.reset();
      refreshMutation.reset();
      return;
    }
    previewMutation.mutate(
      {
        path: { organizationId },
        content: { 'application/json': { manifestUrl: install.manifestUrl } },
      },
      {
        onSuccess: (data) => setPreview(data),
      }
    );
  }, [open]);

  const handleConfirm = () => {
    refreshMutation.mutate(
      {
        path: { organizationId, installId: install.id },
      },
      { onSuccess: onClose }
    );
  };

  const currentScopes = new Set(install.scopes);
  const requested = preview?.requestedScopes ?? [];
  const addedScopes = requested.filter((scope) => !currentScopes.has(scope));
  const keptScopes = requested.filter((scope) => currentScopes.has(scope));
  const removedScopes = install.scopes.filter(
    (scope) => !requested.includes(scope)
  );

  const currentEvents = new Set(install.webhookEvents);
  const requestedEvents = preview?.requestedWebhookEvents ?? [];
  const addedEvents = requestedEvents.filter((e) => !currentEvents.has(e));
  const keptEvents = requestedEvents.filter((e) => currentEvents.has(e));
  const removedEvents = install.webhookEvents.filter(
    (e) => !requestedEvents.includes(e)
  );

  return (
    <Dialog
      open={open}
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

            {addedScopes.length > 0 && (
              <Box mb={2}>
                <Typography variant="body2" mb={1}>
                  <T
                    keyName="organization_apps_refresh_added_scopes"
                    defaultValue="New permissions requested:"
                  />
                </Typography>
                <Box display="flex" flexWrap="wrap" gap={1}>
                  {addedScopes.map((scope) => (
                    <Chip
                      key={scope}
                      size="small"
                      color="warning"
                      label={scope}
                      data-cy="organization-apps-refresh-scope-added"
                    />
                  ))}
                </Box>
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
                <Box display="flex" flexWrap="wrap" gap={1}>
                  {removedScopes.map((scope) => (
                    <Chip
                      key={scope}
                      size="small"
                      variant="outlined"
                      label={scope}
                      data-cy="organization-apps-refresh-scope-removed"
                    />
                  ))}
                </Box>
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
                <Box display="flex" flexWrap="wrap" gap={1}>
                  {keptScopes.map((scope) => (
                    <Chip
                      key={scope}
                      size="small"
                      label={scope}
                      data-cy="organization-apps-refresh-scope-kept"
                    />
                  ))}
                </Box>
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

            {addedEvents.length > 0 && (
              <Box mt={2} mb={2}>
                <Typography variant="body2" mb={1}>
                  <T
                    keyName="organization_apps_refresh_added_events"
                    defaultValue="New webhook events:"
                  />
                </Typography>
                <Box display="flex" flexWrap="wrap" gap={1}>
                  {addedEvents.map((event) => (
                    <Chip
                      key={event}
                      size="small"
                      color="warning"
                      label={event}
                      data-cy="organization-apps-refresh-event-added"
                    />
                  ))}
                </Box>
              </Box>
            )}

            {removedEvents.length > 0 && (
              <Box mb={2}>
                <Typography variant="body2" mb={1}>
                  <T
                    keyName="organization_apps_refresh_removed_events"
                    defaultValue="Webhook events no longer requested:"
                  />
                </Typography>
                <Box display="flex" flexWrap="wrap" gap={1}>
                  {removedEvents.map((event) => (
                    <Chip
                      key={event}
                      size="small"
                      variant="outlined"
                      label={event}
                      data-cy="organization-apps-refresh-event-removed"
                    />
                  ))}
                </Box>
              </Box>
            )}

            {keptEvents.length > 0 && (
              <Box mb={1}>
                <Typography variant="body2" mb={1}>
                  <T
                    keyName="organization_apps_refresh_kept_events"
                    defaultValue="Webhook events already subscribed:"
                  />
                </Typography>
                <Box display="flex" flexWrap="wrap" gap={1}>
                  {keptEvents.map((event) => (
                    <Chip
                      key={event}
                      size="small"
                      label={event}
                      data-cy="organization-apps-refresh-event-kept"
                    />
                  ))}
                </Box>
              </Box>
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
