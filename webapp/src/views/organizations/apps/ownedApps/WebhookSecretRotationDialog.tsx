import { useState } from 'react';
import {
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Typography,
} from '@mui/material';
import { T } from '@tolgee/react';

import LoadingButton from 'tg.component/common/form/LoadingButton';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { components } from 'tg.service/apiSchema.generated';
import { AppCredentialsDisclosure } from 'tg.component/apps/AppCredentialsDisclosure';

type AppWebhookSecretModel = components['schemas']['AppWebhookSecretModel'];

type Props = {
  organizationId: number;
  appId: number;
  appName: string;
  onClose: () => void;
};

const OWNED_PREFIX = '/v2/organizations/{organizationId}/owned-apps';

/**
 * One-step webhook-secret rotation: mint the new secret (delivered to the app signed with the old
 * one, so a running app adopts it automatically) and show it once. The app keeps accepting the
 * previous secret during the overlap and drops it on its own next rotation.
 */
export const WebhookSecretRotationDialog = ({
  organizationId,
  appId,
  appName,
  onClose,
}: Props) => {
  const [rotated, setRotated] = useState<AppWebhookSecretModel | null>(null);

  const rotateMutation = useApiMutation({
    url: '/v2/organizations/{organizationId}/owned-apps/{appId}/webhook-secret',
    method: 'post',
    invalidatePrefix: OWNED_PREFIX,
  });

  const handleRotate = () =>
    rotateMutation.mutate(
      { path: { organizationId, appId } },
      { onSuccess: (data) => setRotated(data) }
    );

  return (
    <Dialog
      open
      onClose={onClose}
      maxWidth="sm"
      fullWidth
      data-cy="webhook-secret-rotation-dialog"
    >
      <DialogTitle>
        <T
          keyName="webhook_rotation_title"
          defaultValue="Rotate webhook signing secret"
        />
      </DialogTitle>
      <DialogContent>
        <Typography variant="body2" color="text.secondary" mb={2}>
          <T
            keyName="webhook_rotation_intro"
            defaultValue="The webhook signing secret is what {name} uses to check that a lifecycle delivery really came from Tolgee. Rotating mints a new one and delivers it to the app signed with the old one, so a running app adopts it automatically."
            params={{ name: appName }}
          />
        </Typography>

        {rotated?.secret && (
          <Box mb={1}>
            <AppCredentialsDisclosure
              webhookSecret={rotated.secret}
              delivery={rotated.delivery}
              dataCy="webhook-secret-rotation-new"
            />
          </Box>
        )}
      </DialogContent>
      <DialogActions>
        {rotated ? (
          <Button
            data-cy="webhook-secret-rotation-done"
            variant="contained"
            color="primary"
            onClick={onClose}
          >
            <T keyName="webhook_rotation_done_button" defaultValue="Done" />
          </Button>
        ) : (
          <>
            <Button data-cy="webhook-secret-rotation-cancel" onClick={onClose}>
              <T keyName="global_cancel_button" defaultValue="Cancel" />
            </Button>
            <LoadingButton
              data-cy="webhook-secret-rotation-rotate"
              variant="contained"
              color="primary"
              loading={rotateMutation.isLoading}
              onClick={handleRotate}
            >
              <T
                keyName="webhook_rotation_rotate_button"
                defaultValue="Rotate now"
              />
            </LoadingButton>
          </>
        )}
      </DialogActions>
    </Dialog>
  );
};
