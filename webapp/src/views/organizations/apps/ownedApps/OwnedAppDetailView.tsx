import { useState } from 'react';
import {
  Box,
  Button,
  Divider,
  FormControlLabel,
  IconButton,
  Switch,
  Tooltip,
  Typography,
  styled,
} from '@mui/material';
import { Eye, EyeOff, RefreshCcw01, Trash01 } from '@untitled-ui/icons-react';
import { Link, useHistory, useParams } from 'react-router-dom';
import { T, useTranslate } from '@tolgee/react';

import { BaseOrganizationSettingsView } from '../../components/BaseOrganizationSettingsView';
import { useOrganization } from '../../useOrganization';
import { LINKS, PARAMS } from 'tg.constants/links';
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import { confirmation } from 'tg.hooks/confirmation';
import { useIsAdmin } from 'tg.globalContext/helpers';
import { useDateFormatter } from 'tg.hooks/useLocale';
import { ClipboardCopyInput } from 'tg.component/common/ClipboardCopyInput';
import { messageService } from 'tg.service/MessageService';
import { DangerButton } from 'tg.component/DangerZone/DangerButton';
import { DangerZone } from 'tg.component/DangerZone/DangerZone';
import { AppManifestHealth } from './AppManifestHealth';
import { ClientSecretRotationDialog } from './ClientSecretRotationDialog';
import { WebhookSecretRotationDialog } from './WebhookSecretRotationDialog';

const OWNED_PREFIX = '/v2/organizations/{organizationId}/owned-apps';

const StyledField = styled('div')`
  display: grid;
  gap: ${({ theme }) => theme.spacing(0.25)};
`;

const StyledSecretRow = styled('div')`
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: ${({ theme }) => theme.spacing(2)};
  padding: ${({ theme }) => theme.spacing(1, 0)};
  & + & {
    border-top: 1px solid ${({ theme }) => theme.palette.divider};
  }
`;

const StyledMono = styled('span')`
  font-family: monospace;
`;

const StyledAdminBox = styled('div')`
  border: 1px solid ${({ theme }) => theme.palette.error.main};
  border-radius: 8px;
  padding: ${({ theme }) => theme.spacing(2)};
  display: grid;
  gap: ${({ theme }) => theme.spacing(1)};
`;

const StyledOrgRow = styled('div')`
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: ${({ theme }) => theme.spacing(2)};
  padding: ${({ theme }) => theme.spacing(1, 0)};
  & + & {
    border-top: 1px solid ${({ theme }) => theme.palette.divider};
  }
`;

const Field = ({
  label,
  value,
}: {
  label: React.ReactNode;
  value: React.ReactNode;
}) => (
  <StyledField>
    <Typography variant="caption" color="text.secondary">
      {label}
    </Typography>
    <Typography variant="body2">{value}</Typography>
  </StyledField>
);

export const OwnedAppDetailView = () => {
  const organization = useOrganization();
  const isAdmin = useIsAdmin();
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

  const installationsLoadable = useApiQuery({
    url: '/v2/organizations/{organizationId}/owned-apps/{appId}/installations',
    method: 'get',
    path: { organizationId, appId },
    options: { enabled: Boolean(organization) && isAdmin },
  });

  const revokeSecretMutation = useApiMutation({
    url: '/v2/organizations/{organizationId}/owned-apps/{appId}/secrets/{secretId}',
    method: 'delete',
    invalidatePrefix: OWNED_PREFIX,
  });
  const availabilityMutation = useApiMutation({
    url: '/v2/organizations/{organizationId}/owned-apps/{appId}/availability',
    method: 'put',
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
  const installingOrgs =
    installationsLoadable.data?._embedded?.installingOrganizations ?? [];

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
          defaultValue="{value} stops authenticating immediately. Anything still using it — an app instance that has not switched to the newer secret yet — loses access."
          params={{ value: `${secret.prefix}…${secret.suffix}` }}
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
          defaultValue="{name} is uninstalled from every organization that installed it, this one included — {count, plural, one {# organization} other {# organizations}} right now. Its app-level credentials stop working immediately, and every project it is enabled in loses it. This cannot be undone."
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
          {/* Info (no heading — it's clearly the app's info) */}
          <Field
            label={<T keyName="owned_app_field_app_id" defaultValue="App id" />}
            value={app.appId}
          />
          <Field
            label={
              <T
                keyName="owned_app_field_manifest_url"
                defaultValue="Manifest URL"
              />
            }
            value={app.manifestUrl}
          />
          <Field
            label={
              <T keyName="owned_app_field_base_url" defaultValue="Base URL" />
            }
            value={app.baseUrl}
          />
          <Field
            label={
              <T
                keyName="owned_app_field_installs"
                defaultValue="Installed by"
              />
            }
            value={
              <T
                keyName="owned_apps_install_count"
                defaultValue="Installed by {count, plural, one {# organization} other {# organizations}}"
                params={{ count: app.installCount }}
              />
            }
          />
          <Box mt={0.5}>
            <AppManifestHealth app={app} />
          </Box>

          <Divider sx={{ my: 1.5 }} />

          {/* Security */}
          <Box data-cy="owned-app-security-section" display="grid" gap={1}>
            <Typography variant="h6">
              <T keyName="owned_app_security_title" defaultValue="Security" />
            </Typography>

            {app.clientId && (
              <Field
                label={
                  <T
                    keyName="owned_app_secrets_client_id_label"
                    defaultValue="Client ID"
                  />
                }
                value={
                  <StyledMono data-cy="owned-app-secrets-client-id">
                    {app.clientId}
                  </StyledMono>
                }
              />
            )}

            <Box
              display="flex"
              alignItems="center"
              justifyContent="space-between"
              mt={1}
            >
              <Typography variant="subtitle2">
                <T
                  keyName="owned_app_secrets_list_title"
                  defaultValue="Client secret"
                />
              </Typography>
              <Tooltip title={t('owned_app_secrets_rotate_button', 'Rotate')}>
                <IconButton
                  size="small"
                  color="primary"
                  data-cy="owned-app-secrets-rotate"
                  onClick={() => setClientDialogOpen(true)}
                >
                  <RefreshCcw01 width={18} height={18} />
                </IconButton>
              </Tooltip>
            </Box>
            <Typography variant="body2" color="text.secondary">
              <T
                keyName="owned_app_secrets_short"
                defaultValue="How the app proves who it is. Rotate it if it may have leaked: a new secret is created, you move the app over, then the old one is revoked."
              />
            </Typography>

            {activeSecrets.map((secret) => (
              <StyledSecretRow
                key={secret.id}
                data-cy="owned-app-secrets-item"
                data-cy-prefix={secret.prefix}
                data-cy-expiring={secret.expiresAt ? 'true' : 'false'}
              >
                <StyledMono data-cy="owned-app-secrets-item-value">
                  {secret.prefix}…{secret.suffix}
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
                    <Typography variant="caption" color="text.secondary" noWrap>
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
                        size="small"
                        data-cy="owned-app-secret-revoke"
                        disabled={revokeSecretMutation.isLoading}
                        onClick={() => handleRevoke(secret)}
                      >
                        <Trash01 width={18} height={18} />
                      </IconButton>
                    </Tooltip>
                  )}
                </Box>
              </StyledSecretRow>
            ))}

            <Typography variant="subtitle2" mt={2}>
              <T
                keyName="owned_app_webhook_secret_title"
                defaultValue="Webhook signing secret"
              />
            </Typography>
            <Typography variant="body2" color="text.secondary">
              <T
                keyName="owned_app_webhook_secret_short"
                defaultValue="Lets the app verify a lifecycle delivery really came from Tolgee."
              />
            </Typography>
            <StyledSecretRow data-cy="owned-app-webhook-secret-row">
              <Box flexGrow={1} minWidth={0}>
                {webhookRevealed && webhookSecretLoadable.data ? (
                  <Box
                    data-cy="owned-app-webhook-secret-value"
                    data-sentry-mask=""
                  >
                    <ClipboardCopyInput
                      value={webhookSecretLoadable.data.secret}
                    />
                  </Box>
                ) : (
                  <StyledMono>••••••••••••••••••••</StyledMono>
                )}
              </Box>
              <Box display="flex" gap={0.5}>
                <Tooltip
                  title={
                    webhookRevealed
                      ? t('owned_app_webhook_secret_hide', 'Hide')
                      : t('owned_app_webhook_secret_reveal', 'Reveal')
                  }
                >
                  <IconButton
                    size="small"
                    data-cy="owned-app-webhook-secret-reveal"
                    onClick={() => setWebhookRevealed((v) => !v)}
                  >
                    {webhookRevealed ? (
                      <EyeOff width={18} height={18} />
                    ) : (
                      <Eye width={18} height={18} />
                    )}
                  </IconButton>
                </Tooltip>
                <Tooltip
                  title={t('owned_app_webhook_secret_rotate_button', 'Rotate')}
                >
                  <IconButton
                    size="small"
                    color="primary"
                    data-cy="owned-app-webhook-secret-rotate"
                    onClick={() => setWebhookDialogOpen(true)}
                  >
                    <RefreshCcw01 width={18} height={18} />
                  </IconButton>
                </Tooltip>
              </Box>
            </StyledSecretRow>
          </Box>

          {/* Server admin */}
          {isAdmin && (
            <>
              <Divider sx={{ my: 1.5 }} />
              <StyledAdminBox data-cy="owned-app-admin-section">
                <Typography variant="h6" color="error">
                  <T
                    keyName="owned_app_admin_title"
                    defaultValue="Server admin"
                  />
                </Typography>
                <Typography variant="caption" color="error">
                  <T
                    keyName="owned_apps_admin_note"
                    defaultValue="You see this because you are a server admin."
                  />
                </Typography>

                <FormControlLabel
                  control={
                    <Switch
                      data-cy="owned-app-availability-switch"
                      checked={app.availableToAllOrganizations}
                      disabled={availabilityMutation.isLoading}
                      onChange={(e) =>
                        availabilityMutation.mutate({
                          path: { organizationId, appId },
                          query: { available: e.target.checked },
                        })
                      }
                    />
                  }
                  label={
                    <T
                      keyName="owned_apps_availability_label"
                      defaultValue="Available to every organization on this server"
                    />
                  }
                />

                <Typography variant="subtitle2" mt={1}>
                  <T
                    keyName="owned_app_installations_title"
                    defaultValue="Installed by"
                  />
                </Typography>
                {installingOrgs.length === 0 && (
                  <Typography variant="body2" color="text.secondary">
                    <T
                      keyName="manage_installations_empty"
                      defaultValue="No organization has this app installed."
                    />
                  </Typography>
                )}
                {installingOrgs.map((org) => (
                  <StyledOrgRow
                    key={org.id}
                    data-cy="owned-app-installations-item"
                    data-cy-slug={org.slug}
                  >
                    <Box minWidth={0}>
                      <Typography variant="body2" noWrap>
                        {org.name}
                      </Typography>
                      <Typography
                        variant="caption"
                        color="text.secondary"
                        noWrap
                      >
                        <T
                          keyName="owned_app_installation_org_id"
                          defaultValue="{slug} · ID {id}"
                          params={{ slug: org.slug, id: org.id }}
                        />
                      </Typography>
                    </Box>
                    <Button
                      size="small"
                      variant="outlined"
                      component={Link}
                      data-cy="owned-app-installations-item-manage"
                      to={LINKS.ORGANIZATION_APPS.build({
                        [PARAMS.ORGANIZATION_SLUG]: org.slug,
                      })}
                    >
                      <T
                        keyName="manage_installations_manage_button"
                        defaultValue="Manage organization's app"
                      />
                    </Button>
                  </StyledOrgRow>
                ))}
              </StyledAdminBox>
            </>
          )}

          {/* Removal is the owner's action, not a server-admin one — the backend gates it on
              organization ownership, so it must not hide inside the admin-only box. */}
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
                    defaultValue="Removes the app from every organization that installed it — {count, plural, one {# organization} other {# organizations}} right now — and revokes its credentials. Projects using it lose it immediately. This cannot be undone."
                    params={{ count: app.installCount }}
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
