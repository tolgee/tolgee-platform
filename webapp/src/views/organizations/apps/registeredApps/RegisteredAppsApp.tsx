import { useState } from 'react';
import {
  Box,
  Button,
  Chip,
  IconButton,
  Tooltip,
  Typography,
  styled,
} from '@mui/material';
import { Eye, EyeOff, RefreshCcw01, Trash01 } from '@untitled-ui/icons-react';
import { T } from '@tolgee/react';

import { useOrganization } from 'tg.views/organizations/useOrganization';
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import { confirmation } from 'tg.hooks/confirmation';
import { components } from 'tg.service/apiSchema.generated';

import { RegisterAppDialog } from './RegisterAppDialog';
import { RefreshAppDialog } from './RefreshAppDialog';

type AppInstallModel = components['schemas']['AppInstallModel'];

const StyledContainer = styled('div')`
  display: grid;
  border-radius: 4px;
  border: 1px solid ${({ theme }) => theme.palette.divider};
  background: ${({ theme }) => theme.palette.background.paper};
`;

const StyledHeader = styled('div')`
  padding: 20px;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
`;

const StyledItem = styled('div')`
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-top: 1px solid ${({ theme }) => theme.palette.divider};
  padding: 12px 20px;
  gap: 16px;
`;

const StyledItemMeta = styled('div')`
  display: grid;
  gap: 4px;
  min-width: 0;
`;

const StyledManifestUrl = styled(Typography)`
  color: ${({ theme }) => theme.palette.text.secondary};
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
`;

const StyledModuleChips = styled('div')`
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 4px;
`;

export const RegisteredAppsApp = () => {
  const organization = useOrganization();
  const [dialogOpen, setDialogOpen] = useState(false);
  const [refreshing, setRefreshing] = useState<AppInstallModel | null>(null);

  const apps = useApiQuery({
    url: '/v2/organizations/{organizationId}/apps',
    method: 'get',
    path: {
      organizationId: organization?.id ?? 0,
    },
    options: {
      enabled: Boolean(organization),
    },
  });

  const removeMutation = useApiMutation({
    url: '/v2/organizations/{organizationId}/apps/{installId}',
    method: 'delete',
    invalidatePrefix: '/v2/organizations/{organizationId}/apps',
  });

  if (!organization) {
    return null;
  }

  const handleRemove = (installId: number, name: string) => {
    confirmation({
      title: (
        <T
          keyName="organization_apps_remove_confirm_title"
          defaultValue="Remove app?"
        />
      ),
      message: (
        <T
          keyName="organization_apps_remove_confirm_message"
          defaultValue="Remove {name} from this organization?"
          params={{ name }}
        />
      ),
      onConfirm: () => {
        removeMutation.mutate({
          path: { organizationId: organization.id, installId },
        });
      },
    });
  };

  const items = apps.data?._embedded?.appInstalls ?? [];

  return (
    <>
      <StyledContainer data-cy="organization-apps-registered-section">
        <StyledHeader>
          <Box>
            <Typography variant="h6">
              <T
                keyName="organization_apps_registered_title"
                defaultValue="Custom apps"
              />
            </Typography>
            <Typography variant="body2" color="text.secondary">
              <T
                keyName="organization_apps_registered_description"
                defaultValue="Register Tolgee apps from a manifest URL."
              />
            </Typography>
          </Box>
          <Button
            data-cy="organization-apps-register-button"
            variant="contained"
            color="primary"
            onClick={() => setDialogOpen(true)}
          >
            <T
              keyName="organization_apps_register_button"
              defaultValue="Register app"
            />
          </Button>
        </StyledHeader>

        {items.map((item) => {
          const dashboardPages = item.modules?.['project-dashboard-page'] ?? [];
          return (
            <StyledItem key={item.id} data-cy="organization-apps-item">
              <StyledItemMeta>
                <Typography variant="subtitle1">
                  {item.name}{' '}
                  <Typography
                    component="span"
                    variant="body2"
                    color="text.secondary"
                  >
                    v{item.version}
                  </Typography>
                </Typography>
                <StyledManifestUrl variant="body2">
                  {item.manifestUrl}
                </StyledManifestUrl>
                {dashboardPages.length > 0 && (
                  <StyledModuleChips>
                    {dashboardPages.map((module) => (
                      <Chip
                        key={module.key}
                        size="small"
                        label={`${module.icon} ${module.title}`}
                      />
                    ))}
                  </StyledModuleChips>
                )}
                {item.scopes.length > 0 && (
                  <StyledModuleChips data-cy="organization-apps-item-scopes">
                    {item.scopes.map((scope) => (
                      <Chip
                        key={scope}
                        size="small"
                        variant="outlined"
                        label={scope}
                      />
                    ))}
                  </StyledModuleChips>
                )}
                {item.webhookEvents.length > 0 && (
                  <StyledModuleChips data-cy="organization-apps-item-events">
                    {item.webhookEvents.map((event) => (
                      <Chip
                        key={event}
                        size="small"
                        color="info"
                        variant="outlined"
                        label={event}
                      />
                    ))}
                  </StyledModuleChips>
                )}
                <CredentialsRow install={item} />
              </StyledItemMeta>
              <Box display="flex" gap={1}>
                <Tooltip
                  title={
                    <T
                      keyName="organization_apps_refresh_tooltip"
                      defaultValue="Refresh manifest"
                    />
                  }
                >
                  <IconButton
                    data-cy="organization-apps-item-refresh"
                    onClick={() => setRefreshing(item)}
                  >
                    <RefreshCcw01 />
                  </IconButton>
                </Tooltip>
                <Tooltip
                  title={
                    <T
                      keyName="organization_apps_remove_tooltip"
                      defaultValue="Remove app"
                    />
                  }
                >
                  <IconButton
                    data-cy="organization-apps-item-remove"
                    onClick={() => handleRemove(item.id, item.name)}
                    disabled={removeMutation.isLoading}
                  >
                    <Trash01 />
                  </IconButton>
                </Tooltip>
              </Box>
            </StyledItem>
          );
        })}
      </StyledContainer>

      <RegisterAppDialog
        open={dialogOpen}
        onClose={() => setDialogOpen(false)}
      />

      {refreshing && (
        <RefreshAppDialog
          open
          organizationId={organization.id}
          install={refreshing}
          onClose={() => setRefreshing(null)}
        />
      )}
    </>
  );
};

const StyledCredentialsRow = styled('div')`
  display: grid;
  grid-template-columns: minmax(0, 120px) minmax(0, 1fr);
  gap: 4px 12px;
  margin-top: 8px;
  align-items: center;
  font-size: 0.8rem;
`;

const StyledCredentialValue = styled('span')`
  font-family: monospace;
  word-break: break-all;
  color: ${({ theme }) => theme.palette.text.secondary};
`;

const CredentialsRow = ({
  install,
}: {
  install: components['schemas']['AppInstallModel'];
}) => {
  const [revealed, setRevealed] = useState(false);
  if (!install.clientId && !install.webhookSecret) return null;
  return (
    <StyledCredentialsRow data-cy="organization-apps-item-credentials">
      <Typography variant="caption" color="text.secondary">
        <T
          keyName="organization_apps_item_client_id"
          defaultValue="Client ID"
        />
      </Typography>
      <StyledCredentialValue data-cy="organization-apps-item-client-id">
        {install.clientId ?? '—'}
      </StyledCredentialValue>

      <Typography variant="caption" color="text.secondary">
        <T
          keyName="organization_apps_item_client_secret"
          defaultValue="Client secret"
        />
      </Typography>
      <StyledCredentialValue data-cy="organization-apps-item-client-secret">
        {install.clientSecretPrefix
          ? `${install.clientSecretPrefix}… (hidden — re-register to rotate)`
          : '—'}
      </StyledCredentialValue>

      <Typography variant="caption" color="text.secondary">
        <T
          keyName="organization_apps_item_webhook_secret"
          defaultValue="Webhook secret"
        />
      </Typography>
      <Box display="flex" alignItems="center" gap={1}>
        <StyledCredentialValue
          data-cy="organization-apps-item-webhook-secret"
          sx={{ minWidth: 0, flex: 1 }}
        >
          {revealed
            ? install.webhookSecret ?? '—'
            : install.webhookSecret
            ? '••••••••••••'
            : '—'}
        </StyledCredentialValue>
        {install.webhookSecret && (
          <Tooltip
            title={
              revealed ? (
                <T
                  keyName="organization_apps_item_hide_secret"
                  defaultValue="Hide"
                />
              ) : (
                <T
                  keyName="organization_apps_item_show_secret"
                  defaultValue="Show"
                />
              )
            }
          >
            <IconButton
              size="small"
              onClick={() => setRevealed((v) => !v)}
              data-cy="organization-apps-item-webhook-secret-toggle"
            >
              {revealed ? (
                <EyeOff width={14} height={14} />
              ) : (
                <Eye width={14} height={14} />
              )}
            </IconButton>
          </Tooltip>
        )}
      </Box>
    </StyledCredentialsRow>
  );
};
