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
import { RefreshCcw01, Trash01 } from '@untitled-ui/icons-react';
import { T } from '@tolgee/react';

import { useOrganization } from 'tg.views/organizations/useOrganization';
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import { confirmation } from 'tg.hooks/confirmation';

import { RegisterAppDialog } from './RegisterAppDialog';

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

  const refreshMutation = useApiMutation({
    url: '/v2/organizations/{organizationId}/apps/{installId}/refresh',
    method: 'post',
    invalidatePrefix: '/v2/organizations/{organizationId}/apps',
  });

  const removeMutation = useApiMutation({
    url: '/v2/organizations/{organizationId}/apps/{installId}',
    method: 'delete',
    invalidatePrefix: '/v2/organizations/{organizationId}/apps',
  });

  if (!organization) {
    return null;
  }

  const handleRefresh = (installId: number) => {
    refreshMutation.mutate({
      path: { organizationId: organization.id, installId },
    });
  };

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
                    onClick={() => handleRefresh(item.id)}
                    disabled={refreshMutation.isLoading}
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
    </>
  );
};
