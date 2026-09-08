import React, { useState } from 'react';
import {
  Box,
  Divider,
  IconButton,
  Tooltip,
  Typography,
  styled,
} from '@mui/material';
import {
  Copy01,
  Eye,
  EyeOff,
  RefreshCcw01,
  Trash01,
} from '@untitled-ui/icons-react';
import { useHistory, useParams } from 'react-router-dom';
import { T, useTranslate } from '@tolgee/react';

import { BaseOrganizationSettingsView } from '../../components/BaseOrganizationSettingsView';
import { useOrganization } from '../../useOrganization';
import { LINKS, PARAMS } from 'tg.constants/links';
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import { confirmation } from 'tg.hooks/confirmation';
import { useDateFormatter } from 'tg.hooks/useLocale';
import { messageService } from 'tg.service/MessageService';
import { DangerButton } from 'tg.component/DangerZone/DangerButton';
import { DangerZone } from 'tg.component/DangerZone/DangerZone';
import { AppAvatar } from 'tg.component/apps/AppAvatar';
import { AppField } from 'tg.component/apps/AppField';
import { AppManifestHealth } from './AppManifestHealth';
import { ClientSecretRotationDialog } from './ClientSecretRotationDialog';
import { OwnedAppHealthChip } from './OwnedAppHealthChip';
import { WebhookSecretRotationDialog } from './WebhookSecretRotationDialog';

const OWNED_PREFIX = '/v2/organizations/{organizationId}/owned-apps';

const StyledMono = styled('span')`
  font-family: monospace;
  font-size: 13px;
  color: ${({ theme }) => theme.palette.text.primary};
`;

const StyledEllipsisMono = styled('span')`
  font-family: monospace;
  font-size: 13px;
  color: ${({ theme }) => theme.palette.text.primary};
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
`;

const StyledMetaLine = styled('div')`
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: ${({ theme }) => theme.spacing(0.75)};
  color: ${({ theme }) => theme.palette.text.secondary};
  font-size: 13px;
`;

const StyledFieldGrid = styled('div')`
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: ${({ theme }) => theme.spacing(1.25, 3)};
`;

const StyledCredentialRow = styled('div')`
  display: grid;
  grid-template-columns: 220px 1fr auto;
  gap: ${({ theme }) => theme.spacing(2)};
  align-items: start;
  padding: ${({ theme }) => theme.spacing(1.75, 0)};
  & + & {
    border-top: 1px solid ${({ theme }) => theme.palette.divider};
  }
  @media (max-width: 700px) {
    grid-template-columns: 1fr;
  }
`;

const StyledSecretLine = styled('div')`
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: ${({ theme }) => theme.spacing(1.5)};
  min-height: 30px;
  & + & {
    border-top: 1px dashed ${({ theme }) => theme.palette.divider};
  }
`;

const CredentialRow = ({
  title,
  description,
  actions,
  children,
  dataCy,
}: {
  title: React.ReactNode;
  description: React.ReactNode;
  actions?: React.ReactNode;
  children: React.ReactNode;
  dataCy: string;
}) => (
  <StyledCredentialRow data-cy={dataCy}>
    <Box display="grid" gap={0.25}>
      <Typography variant="subtitle2">{title}</Typography>
      <Typography variant="caption" color="text.secondary">
        {description}
      </Typography>
    </Box>
    <Box minWidth={0} display="grid" gap={0.5} alignContent="center">
      {children}
    </Box>
    <Box display="flex" gap={0.5}>
      {actions}
    </Box>
  </StyledCredentialRow>
);

export const OwnedAppDetailView = () => {
  const organization = useOrganization();
  const { t } = useTranslate();
  const history = useHistory();
  const params = useParams<Record<string, string>>();
  const appId = Number(params[PARAMS.APP_ID]);
  const organizationId = organization?.id ?? 0;

  const formatDate = useDateFormatter();
  const [clientDialogOpen, setClientDialogOpen] = useState(false);
  const [webhookRevealed, setWebhookRevealed] = useState(false);
  const [webhookDialogOpen, setWebhookDialogOpen] = useState(false);

  const appLoadable = useApiQuery({
    url: '/v2/organizations/{organizationId}/owned-apps/{appId}',
    method: 'get',
    path: { organizationId, appId },
    options: { enabled: Boolean(organization) },
  });

  const secretsLoadable = useApiQuery({
    url: '/v2/organizations/{organizationId}/owned-apps/{appId}/secrets',
    method: 'get',
    path: { organizationId, appId },
    options: { enabled: Boolean(organization) },
  });

  const webhookSecretLoadable = useApiQuery({
    url: '/v2/organizations/{organizationId}/owned-apps/{appId}/webhook-secret',
    method: 'get',
    path: { organizationId, appId },
    options: { enabled: Boolean(organization) && webhookRevealed },
  });

  const revokeSecretMutation = useApiMutation({
    url: '/v2/organizations/{organizationId}/owned-apps/{appId}/secrets/{secretId}',
    method: 'delete',
    invalidatePrefix: OWNED_PREFIX,
  });
  const removeMutation = useApiMutation({
    url: '/v2/organizations/{organizationId}/owned-apps/{appId}',
    method: 'delete',
    invalidatePrefix: [
      '/v2/organizations/{organizationId}/apps',
      '/v2/organizations/{organizationId}/owned-apps',
    ],
  });

  const app = appLoadable.data;
  // Active = not revoked and not past its expiry. Normally one; a second (with an expiry) shows only
  // while a rotation's grace window is open. Oldest first, so it reads current → expiring.
  const now = Date.now();
  const activeSecrets = (secretsLoadable.data?._embedded?.appSecrets ?? [])
    .filter(
      (secret) =>
        !secret.revokedAt && (!secret.expiresAt || secret.expiresAt > now)
    )
    .sort((a, b) => a.createdAt - b.createdAt);

  const handleCopy = (value: string | undefined | null) => {
    if (!value) return;
    navigator.clipboard.writeText(value);
    messageService.success(
      <T keyName="owned_app_value_copied" defaultValue="Copied to clipboard" />
    );
  };

  const handleRevoke = (secret: (typeof activeSecrets)[number]) =>
    confirmation({
      title: (
        <T
          keyName="owned_app_secret_revoke_confirm_title"
          defaultValue="Revoke this secret?"
        />
      ),
      message: (
        <T
          keyName="owned_app_secret_revoke_confirm_message"
          defaultValue="{value} stops authenticating immediately. Anything still using it, such as an app instance that has not switched to the newer secret yet, loses access."
          params={{ value: secret.name }}
        />
      ),
      onConfirm: () =>
        revokeSecretMutation.mutate({
          path: { organizationId, appId, secretId: secret.id },
          query: { force: false },
        }),
    });

  const ownedListLink = organization
    ? LINKS.ORGANIZATION_APPS_OWNED.build({
        [PARAMS.ORGANIZATION_SLUG]: organization.slug,
      })
    : '';

  const handleRemoveEverywhere = () => {
    if (!app) return;
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
          defaultValue="{name} is uninstalled from every organization that installed it, this one included ({count, plural, one {# organization} other {# organizations}} right now). Its app-level credentials stop working immediately, and every project it is enabled in loses it. This cannot be undone."
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
      onConfirm: () =>
        removeMutation.mutate(
          { path: { organizationId, appId } },
          {
            onSuccess() {
              history.push(ownedListLink);
              messageService.success(
                <T
                  keyName="owned_app_removed_message"
                  defaultValue="App removed"
                />
              );
            },
          }
        ),
    });
  };

  const manifestFailing = Boolean(
    app && (app.manifestFailureCount > 0 || app.unhealthySince)
  );

  return (
    <BaseOrganizationSettingsView
      windowTitle={app?.name ?? t('organization_apps_title')}
      link={LINKS.ORGANIZATION_APP}
      title={app?.name ?? t('organization_apps_title')}
      navigation={
        organization
          ? [
              [t('organization_apps_title'), ownedListLink],
              [
                app?.name ?? '',
                LINKS.ORGANIZATION_APP.build({
                  [PARAMS.ORGANIZATION_SLUG]: organization.slug,
                  [PARAMS.APP_ID]: appId,
                }),
              ],
            ]
          : []
      }
      hideChildrenOnLoading={false}
      maxWidth="normal"
    >
      {app && (
        <Box display="grid" gap={1.5} data-cy="owned-app-detail">
          {/* Identity header */}
          <Box display="flex" alignItems="center" gap={2}>
            <AppAvatar
              icon={app.icon}
              name={app.name}
              appId={app.appId}
              size={56}
            />
            <Box flex={1} minWidth={0} display="grid" gap={0.25}>
              <Box
                display="flex"
                alignItems="center"
                gap={1.25}
                flexWrap="wrap"
              >
                <Typography variant="h6">{app.name}</Typography>
                {app.version && (
                  <Typography variant="body2" color="text.secondary">
                    v{app.version}
                  </Typography>
                )}
                <OwnedAppHealthChip app={app} />
              </Box>
              <StyledMetaLine>
                <span>
                  <T
                    keyName="owned_apps_install_count"
                    defaultValue="Installed by {count, plural, one {# organization} other {# organizations}}"
                    params={{ count: app.installCount }}
                  />
                </span>
                {app.manifestLastCheckedAt != null && (
                  <>
                    <span aria-hidden="true">·</span>
                    <span>
                      <T
                        keyName="owned_app_health_last_checked"
                        defaultValue="Manifest last checked {date}"
                        params={{
                          date: formatDate(app.manifestLastCheckedAt, {
                            dateStyle: 'short',
                            timeStyle: 'short',
                          }),
                        }}
                      />
                    </span>
                  </>
                )}
              </StyledMetaLine>
            </Box>
          </Box>

          {manifestFailing && <AppManifestHealth app={app} />}

          <Divider sx={{ my: 1 }} />

          <StyledFieldGrid>
            <AppField
              label={
                <T keyName="owned_app_field_app_id" defaultValue="App id" />
              }
            >
              <StyledMono data-cy="owned-app-detail-app-id">
                {app.appId}
              </StyledMono>
            </AppField>
            <AppField
              label={
                <T keyName="owned_app_field_base_url" defaultValue="Base URL" />
              }
            >
              <StyledMono>{app.baseUrl}</StyledMono>
            </AppField>
            <Box gridColumn="1 / -1">
              <AppField
                label={
                  <T
                    keyName="owned_app_field_manifest_url"
                    defaultValue="Manifest URL"
                  />
                }
              >
                <StyledMono>{app.manifestUrl}</StyledMono>
              </AppField>
            </Box>
          </StyledFieldGrid>

          <Divider sx={{ my: 1 }} />

          {/* Security — one row grammar per credential */}
          <Box data-cy="owned-app-security-section">
            <Typography variant="h6" mb={0.5}>
              <T keyName="owned_app_security_title" defaultValue="Security" />
            </Typography>

            {app.clientId && (
              <CredentialRow
                dataCy="owned-app-credential-client-id"
                title={
                  <T
                    keyName="owned_app_secrets_client_id_label"
                    defaultValue="Client ID"
                  />
                }
                description={
                  <T
                    keyName="owned_app_client_id_short"
                    defaultValue="Public identifier. Safe to share."
                  />
                }
                actions={null}
              >
                <Box display="flex" alignItems="center" gap={0.5} minWidth={0}>
                  <StyledMono data-cy="owned-app-secrets-client-id">
                    {app.clientId}
                  </StyledMono>
                  <Tooltip title={t('owned_app_copy_button', 'Copy')}>
                    <IconButton
                      data-cy="owned-app-client-id-copy"
                      onClick={() => handleCopy(app.clientId)}
                    >
                      <Copy01 />
                    </IconButton>
                  </Tooltip>
                </Box>
              </CredentialRow>
            )}

            <CredentialRow
              dataCy="owned-app-credential-client-secret"
              title={
                <T
                  keyName="owned_app_secrets_list_title"
                  defaultValue="Client secret"
                />
              }
              description={
                <T
                  keyName="owned_app_secrets_short"
                  defaultValue="How the app proves who it is. Rotate it if it may have leaked."
                />
              }
              actions={
                <Tooltip title={t('owned_app_secrets_rotate_button', 'Rotate')}>
                  <IconButton
                    size="small"
                    color="primary"
                    data-cy="owned-app-secrets-rotate"
                    onClick={() => setClientDialogOpen(true)}
                  >
                    <RefreshCcw01 />
                  </IconButton>
                </Tooltip>
              }
            >
              {activeSecrets.map((secret) => (
                <StyledSecretLine
                  key={secret.id}
                  data-cy="owned-app-secrets-item"
                  data-cy-prefix={secret.name}
                  data-cy-expiring={secret.expiresAt ? 'true' : 'false'}
                >
                  <StyledMono data-cy="owned-app-secrets-item-value">
                    {secret.name}
                  </StyledMono>
                  <Box display="flex" alignItems="center" gap={1}>
                    {secret.expiresAt ? (
                      <Typography variant="caption" color="warning.main" noWrap>
                        <T
                          keyName="owned_app_secret_expires"
                          defaultValue="Expires {date}"
                          params={{
                            date: formatDate(secret.expiresAt, {
                              dateStyle: 'short',
                              timeStyle: 'short',
                            }),
                          }}
                        />
                      </Typography>
                    ) : (
                      <Typography
                        variant="caption"
                        color="text.secondary"
                        noWrap
                      >
                        {secret.lastUsedAt ? (
                          <T
                            keyName="owned_app_secret_last_used"
                            defaultValue="Last used {date}"
                            params={{
                              date: formatDate(secret.lastUsedAt, {
                                dateStyle: 'short',
                                timeStyle: 'short',
                              }),
                            }}
                          />
                        ) : (
                          <T
                            keyName="owned_app_secret_never_used"
                            defaultValue="Never used"
                          />
                        )}
                      </Typography>
                    )}
                    {activeSecrets.length > 1 && (
                      <Tooltip
                        title={t('owned_app_secret_revoke_button', 'Revoke')}
                      >
                        <IconButton
                          data-cy="owned-app-secret-revoke"
                          disabled={revokeSecretMutation.isLoading}
                          onClick={() => handleRevoke(secret)}
                        >
                          <Trash01 />
                        </IconButton>
                      </Tooltip>
                    )}
                  </Box>
                </StyledSecretLine>
              ))}
            </CredentialRow>

            <CredentialRow
              dataCy="owned-app-credential-webhook-secret"
              title={
                <T
                  keyName="owned_app_webhook_secret_title"
                  defaultValue="Webhook signing secret"
                />
              }
              description={
                <T
                  keyName="owned_app_webhook_secret_short"
                  defaultValue="Lets the app verify a lifecycle delivery really came from Tolgee."
                />
              }
              actions={
                <>
                  <Tooltip
                    title={
                      webhookRevealed
                        ? t('owned_app_webhook_secret_hide', 'Hide')
                        : t('owned_app_webhook_secret_reveal', 'Reveal')
                    }
                  >
                    <IconButton
                      data-cy="owned-app-webhook-secret-reveal"
                      onClick={() => setWebhookRevealed((v) => !v)}
                    >
                      {webhookRevealed ? <EyeOff /> : <Eye />}
                    </IconButton>
                  </Tooltip>
                  <Tooltip
                    title={t(
                      'owned_app_webhook_secret_rotate_button',
                      'Rotate'
                    )}
                  >
                    <IconButton
                      color="primary"
                      data-cy="owned-app-webhook-secret-rotate"
                      onClick={() => setWebhookDialogOpen(true)}
                    >
                      <RefreshCcw01 />
                    </IconButton>
                  </Tooltip>
                </>
              }
            >
              <Box data-cy="owned-app-webhook-secret-row">
                {webhookRevealed && webhookSecretLoadable.data ? (
                  <Box
                    display="flex"
                    alignItems="center"
                    gap={0.5}
                    minWidth={0}
                  >
                    <StyledEllipsisMono
                      data-cy="owned-app-webhook-secret-value"
                      data-sentry-mask=""
                    >
                      {webhookSecretLoadable.data.secret}
                    </StyledEllipsisMono>
                    <Tooltip title={t('owned_app_copy_button', 'Copy')}>
                      <IconButton
                        data-cy="owned-app-webhook-secret-copy"
                        onClick={() =>
                          handleCopy(webhookSecretLoadable.data?.secret)
                        }
                      >
                        <Copy01 />
                      </IconButton>
                    </Tooltip>
                  </Box>
                ) : (
                  <StyledMono>••••••••••••••••••••</StyledMono>
                )}
              </Box>
            </CredentialRow>
          </Box>

          {/* Removal is the owner's action, not a server-admin one — the backend gates it on
              organization ownership, so it must not hide inside the admin-only section. */}
          <Box mt={2} mb={1}>
            <Typography variant="h5">
              <T
                keyName="project_settings_danger_zone_title"
                defaultValue="Danger zone"
              />
            </Typography>
          </Box>
          <DangerZone
            actions={[
              {
                description: (
                  <T
                    keyName="owned_app_remove_app_description"
                    defaultValue="Removes the app from every organization that installed it and revokes its credentials. Projects using it lose it immediately. This cannot be undone."
                  />
                ),
                button: (
                  <DangerButton
                    data-cy="owned-app-remove-everywhere"
                    loading={removeMutation.isLoading}
                    onClick={handleRemoveEverywhere}
                  >
                    <T
                      keyName="owned_app_remove_app_button"
                      defaultValue="Remove app"
                    />
                  </DangerButton>
                ),
              },
            ]}
          />

          {clientDialogOpen && (
            <ClientSecretRotationDialog
              organizationId={organizationId}
              appId={appId}
              appName={app.name}
              onClose={() => setClientDialogOpen(false)}
            />
          )}

          {webhookDialogOpen && (
            <WebhookSecretRotationDialog
              organizationId={organizationId}
              appId={appId}
              appName={app.name}
              onClose={() => setWebhookDialogOpen(false)}
            />
          )}
        </Box>
      )}
    </BaseOrganizationSettingsView>
  );
};
