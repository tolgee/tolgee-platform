import { useState } from 'react';
import { Box, IconButton, Tooltip, Typography, styled } from '@mui/material';
import { Key01, Send01, Trash01 } from '@untitled-ui/icons-react';
import { T } from '@tolgee/react';

import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import { confirmation } from 'tg.hooks/confirmation';
import { components } from 'tg.service/apiSchema.generated';

import { AppManifestHealth } from './AppManifestHealth';
import { AppDeliveriesDialog } from './AppDeliveriesDialog';
import { OwnedAppSecretsDialog } from './OwnedAppSecretsDialog';

type OwnedAppModel = components['schemas']['OwnedAppModel'];

type Props = {
  organizationId: number;
};

const StyledContainer = styled('div')`
  display: grid;
  border-radius: ${({ theme }) => theme.shape.borderRadius}px;
  border: 1px solid ${({ theme }) => theme.palette.divider};
  background: ${({ theme }) => theme.palette.background.paper};
`;

const StyledHeader = styled('div')`
  padding: ${({ theme }) => theme.spacing(2.5)};
`;

const StyledItem = styled('div')`
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  border-top: 1px solid ${({ theme }) => theme.palette.divider};
  padding: ${({ theme }) => theme.spacing(1.5, 2.5)};
  gap: ${({ theme }) => theme.spacing(2)};
`;

const StyledItemMeta = styled('div')`
  display: grid;
  gap: ${({ theme }) => theme.spacing(0.5)};
  min-width: 0;
`;

export const OwnedAppsSection = ({ organizationId }: Props) => {
  const [secretsFor, setSecretsFor] = useState<OwnedAppModel | null>(null);
  const [deliveriesFor, setDeliveriesFor] = useState<OwnedAppModel | null>(
    null
  );

  const appsLoadable = useApiQuery({
    url: '/v2/organizations/{organizationId}/owned-apps',
    method: 'get',
    path: { organizationId },
  });

  const removeMutation = useApiMutation({
    url: '/v2/organizations/{organizationId}/owned-apps/{appId}',
    method: 'delete',
    invalidatePrefix: [
      '/v2/organizations/{organizationId}/apps',
      '/v2/organizations/{organizationId}/owned-apps',
    ],
  });

  const items = appsLoadable.data?._embedded?.ownedApps ?? [];

  const handleRemoveEverywhere = (app: OwnedAppModel) => {
    confirmation({
      title: (
        <T
          keyName="owned_apps_remove_confirm_title"
          defaultValue="Remove this app everywhere?"
        />
      ),
      message: (
        <T
          keyName="owned_apps_remove_confirm_message"
          defaultValue="{name} is uninstalled from every organization that installed it, this one included — {count, plural, one {# organization} other {# organizations}} right now. Its app-level and per-install credentials stop working immediately, and every project it is enabled in loses it. This cannot be undone."
          params={{ name: app.name, count: app.installCount }}
        />
      ),
      confirmButtonText: (
        <T
          keyName="owned_apps_remove_confirm_button"
          defaultValue="Remove everywhere"
        />
      ),
      hardModeText: app.name.toUpperCase(),
      onConfirm: () => {
        removeMutation.mutate({ path: { organizationId, appId: app.id } });
      },
    });
  };

  return (
    <>
      <StyledContainer data-cy="organization-owned-apps-section">
        <StyledHeader>
          <Typography variant="h6">
            <T
              keyName="owned_apps_section_title"
              defaultValue="Apps you registered"
            />
          </Typography>
          <Typography variant="body2" color="text.secondary">
            <T
              keyName="owned_apps_section_description"
              defaultValue="Apps this organization registered on this server. You hold their app-level credentials and can take them off every organization that installed them."
            />
          </Typography>
        </StyledHeader>

        {!appsLoadable.isLoading && items.length === 0 && (
          <StyledItem>
            <Typography
              variant="body2"
              color="text.secondary"
              data-cy="organization-owned-apps-empty"
            >
              <T
                keyName="owned_apps_empty"
                defaultValue="This organization has not registered any app yet. Registering happens the first time anyone installs an app from its manifest URL."
              />
            </Typography>
          </StyledItem>
        )}

        {items.map((app) => (
          <StyledItem
            key={app.id}
            data-cy="organization-owned-apps-item"
            data-cy-app-id={app.appId}
          >
            <StyledItemMeta>
              <Typography variant="subtitle1">{app.name}</Typography>
              <Typography variant="body2" color="text.secondary" noWrap>
                {app.manifestUrl}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                <T
                  keyName="owned_apps_install_count"
                  defaultValue="Installed by {count, plural, one {# organization} other {# organizations}}"
                  params={{ count: app.installCount }}
                />
              </Typography>
              <AppManifestHealth app={app} />
            </StyledItemMeta>

            <Box display="flex" gap={1}>
              <Tooltip
                title={
                  <T
                    keyName="owned_apps_secrets_tooltip"
                    defaultValue="App credentials"
                  />
                }
              >
                <IconButton
                  data-cy="organization-owned-apps-item-secrets"
                  onClick={() => setSecretsFor(app)}
                >
                  <Key01 />
                </IconButton>
              </Tooltip>
              <Tooltip
                title={
                  <T
                    keyName="owned_apps_deliveries_tooltip"
                    defaultValue="Lifecycle deliveries"
                  />
                }
              >
                <IconButton
                  data-cy="organization-owned-apps-item-deliveries"
                  onClick={() => setDeliveriesFor(app)}
                >
                  <Send01 />
                </IconButton>
              </Tooltip>
              <Tooltip
                title={
                  <T
                    keyName="owned_apps_remove_tooltip"
                    defaultValue="Remove from every organization"
                  />
                }
              >
                <IconButton
                  data-cy="organization-owned-apps-item-remove"
                  onClick={() => handleRemoveEverywhere(app)}
                  disabled={removeMutation.isLoading}
                >
                  <Trash01 />
                </IconButton>
              </Tooltip>
            </Box>
          </StyledItem>
        ))}
      </StyledContainer>

      {secretsFor && (
        <OwnedAppSecretsDialog
          organizationId={organizationId}
          appId={secretsFor.id}
          appName={secretsFor.name}
          clientId={secretsFor.clientId}
          onClose={() => setSecretsFor(null)}
        />
      )}

      {deliveriesFor && (
        <AppDeliveriesDialog
          organizationId={organizationId}
          appId={deliveriesFor.id}
          appName={deliveriesFor.name}
          onClose={() => setDeliveriesFor(null)}
        />
      )}
    </>
  );
};
