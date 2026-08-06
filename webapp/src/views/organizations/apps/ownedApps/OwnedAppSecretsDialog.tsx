import { useState } from 'react';
import {
  Box,
  Button,
  Chip,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  IconButton,
  styled,
  Tooltip,
  Typography,
} from '@mui/material';
import { Trash01 } from '@untitled-ui/icons-react';
import { T } from '@tolgee/react';

import LoadingButton from 'tg.component/common/form/LoadingButton';
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import { components } from 'tg.service/apiSchema.generated';
import { confirmation } from 'tg.hooks/confirmation';
import { useDateFormatter } from 'tg.hooks/useLocale';
import { AppCredentialsDisclosure } from 'tg.component/apps/AppCredentialsDisclosure';

type AppSecretModel = components['schemas']['AppSecretModel'];

type Props = {
  organizationId: number;
  appId: number;
  appName: string;
  clientId?: string;
  onClose: () => void;
};

const SECRETS_PREFIX = '/v2/organizations/{organizationId}/owned-apps';

const StyledItem = styled('div')`
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: ${({ theme }) => theme.spacing(2)};
  padding: ${({ theme }) => theme.spacing(1.5, 0)};
  & + & {
    border-top: 1px solid ${({ theme }) => theme.palette.divider};
  }
`;

const StyledMeta = styled('div')`
  display: grid;
  gap: ${({ theme }) => theme.spacing(0.25)};
  min-width: 0;
`;

/**
 * The app-level credentials: issued and revoked separately, so the app can pick a new secret
 * up before the old one stops working.
 */
export const OwnedAppSecretsDialog = ({
  organizationId,
  appId,
  appName,
  clientId,
  onClose,
}: Props) => {
  const formatDate = useDateFormatter();
  const format = (value: number) =>
    formatDate(value, { dateStyle: 'short', timeStyle: 'short' });
  const [issued, setIssued] = useState<AppSecretModel | null>(null);

  const secretsLoadable = useApiQuery({
    url: '/v2/organizations/{organizationId}/owned-apps/{appId}/secrets',
    method: 'get',
    path: { organizationId, appId },
  });

  const issueMutation = useApiMutation({
    url: '/v2/organizations/{organizationId}/owned-apps/{appId}/secrets',
    method: 'post',
    invalidatePrefix: SECRETS_PREFIX,
  });

  const revokeMutation = useApiMutation({
    url: '/v2/organizations/{organizationId}/owned-apps/{appId}/secrets/{secretId}',
    method: 'delete',
    invalidatePrefix: SECRETS_PREFIX,
  });

  const secrets = secretsLoadable.data?._embedded?.appSecrets ?? [];

  const handleIssue = () => {
    issueMutation.mutate(
      { path: { organizationId, appId } },
      { onSuccess: (data) => setIssued(data) }
    );
  };

  const handleRevoke = (secret: AppSecretModel) => {
    confirmation({
      title: (
        <T
          keyName="owned_app_secrets_revoke_confirm_title"
          defaultValue="Revoke this secret?"
        />
      ),
      message: (
        <T
          keyName="owned_app_secrets_revoke_confirm_message"
          defaultValue="The secret starting with {prefix} stops administering {name} immediately. Every other secret of the app keeps working."
          params={{ prefix: secret.prefix, name: appName }}
        />
      ),
      confirmButtonText: (
        <T
          keyName="owned_app_secrets_revoke_confirm_button"
          defaultValue="Revoke"
        />
      ),
      onConfirm: () => {
        revokeMutation.mutate({
          path: { organizationId, appId, secretId: secret.id },
        });
      },
    });
  };

  return (
    <Dialog
      open
      onClose={onClose}
      maxWidth="sm"
      fullWidth
      data-cy="owned-app-secrets-dialog"
    >
      <DialogTitle>
        <T
          keyName="owned_app_secrets_dialog_title"
          defaultValue="App credentials of {name}"
          params={{ name: appName }}
        />
      </DialogTitle>

      <DialogContent>
        <Typography variant="body2" color="text.secondary" mb={2}>
          <T
            keyName="owned_app_secrets_description"
            defaultValue="These administer the app across every organization that installed it and grant access to no translation data. They are not the per-install secrets the app uses to read and write translations."
          />
        </Typography>

        {clientId && (
          <Box mb={2}>
            <Typography variant="caption" color="text.secondary">
              <T
                keyName="owned_app_secrets_client_id_label"
                defaultValue="Client ID"
              />
            </Typography>
            <Typography variant="body2" data-cy="owned-app-secrets-client-id">
              {clientId}
            </Typography>
          </Box>
        )}

        {issued?.secret && (
          <Box mb={2}>
            <AppCredentialsDisclosure
              clientSecret={issued.secret}
              dataCy="owned-app-secrets-new"
            />
          </Box>
        )}

        {secretsLoadable.isLoading && (
          <Box display="flex" justifyContent="center" py={4}>
            <CircularProgress size={24} />
          </Box>
        )}

        {secrets.map((secret) => (
          <StyledItem
            key={secret.id}
            data-cy="owned-app-secrets-item"
            data-cy-prefix={secret.prefix}
          >
            <StyledMeta>
              <Box display="flex" alignItems="center" gap={1}>
                <Typography variant="subtitle2">{secret.prefix}…</Typography>
                {secret.revokedAt && (
                  <Chip
                    size="small"
                    color="default"
                    data-cy="owned-app-secrets-item-revoked"
                    label={
                      <T
                        keyName="owned_app_secrets_revoked_chip"
                        defaultValue="Revoked"
                      />
                    }
                  />
                )}
              </Box>
              <Typography variant="caption" color="text.secondary">
                <T
                  keyName="owned_app_secrets_created_at"
                  defaultValue="Created {date}"
                  params={{ date: format(secret.createdAt) }}
                />
              </Typography>
              <Typography variant="caption" color="text.secondary">
                {secret.lastUsedAt ? (
                  <T
                    keyName="owned_app_secrets_last_used_at"
                    defaultValue="Last used {date}"
                    params={{ date: format(secret.lastUsedAt) }}
                  />
                ) : (
                  <T
                    keyName="owned_app_secrets_never_used"
                    defaultValue="Never used"
                  />
                )}
              </Typography>
              {secret.revokedAt && (
                <Typography variant="caption" color="text.secondary">
                  <T
                    keyName="owned_app_secrets_revoked_at"
                    defaultValue="Revoked {date}"
                    params={{ date: format(secret.revokedAt) }}
                  />
                </Typography>
              )}
            </StyledMeta>

            {!secret.revokedAt && (
              <Tooltip
                title={
                  <T
                    keyName="owned_app_secrets_revoke_tooltip"
                    defaultValue="Revoke secret"
                  />
                }
              >
                <IconButton
                  data-cy="owned-app-secrets-revoke"
                  onClick={() => handleRevoke(secret)}
                  disabled={revokeMutation.isLoading}
                >
                  <Trash01 />
                </IconButton>
              </Tooltip>
            )}
          </StyledItem>
        ))}
      </DialogContent>

      <DialogActions>
        <Button data-cy="owned-app-secrets-close" onClick={onClose}>
          <T keyName="global_close_button" defaultValue="Close" />
        </Button>
        <LoadingButton
          data-cy="owned-app-secrets-issue"
          variant="contained"
          color="primary"
          loading={issueMutation.isLoading}
          onClick={handleIssue}
        >
          <T
            keyName="owned_app_secrets_issue_button"
            defaultValue="Issue new secret"
          />
        </LoadingButton>
      </DialogActions>
    </Dialog>
  );
};
