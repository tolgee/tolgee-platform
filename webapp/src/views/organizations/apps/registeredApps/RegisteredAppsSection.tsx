import { useState } from 'react';
import {
  Box,
  Button,
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
import { components } from 'tg.service/apiSchema.generated';

import { RegisterAppDialog } from './RegisterAppDialog';
import { RefreshAppDialog } from './RefreshAppDialog';
import { AppSummary } from 'tg.component/apps/AppSummary';
import { AppChips } from 'tg.component/apps/AppChips';

type AppInstallModel = components['schemas']['AppInstallModel'];

const StyledContainer = styled('div')`
  display: grid;
  border-radius: ${({ theme }) => theme.shape.borderRadius}px;
  border: 1px solid ${({ theme }) => theme.palette.divider};
  background: ${({ theme }) => theme.palette.background.paper};
`;

const StyledHeader = styled('div')`
  padding: ${({ theme }) => theme.spacing(2.5)};
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: ${({ theme }) => theme.spacing(2)};
`;

const StyledItem = styled('div')`
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-top: 1px solid ${({ theme }) => theme.palette.divider};
  padding: ${({ theme }) => theme.spacing(1.5, 2.5)};
  gap: ${({ theme }) => theme.spacing(2)};
`;

const StyledItemMeta = styled('div')`
  display: grid;
  gap: ${({ theme }) => theme.spacing(0.5)};
  min-width: 0;
`;

export const RegisteredAppsSection = () => {
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
                <AppSummary
                  name={item.name}
                  version={item.version}
                  url={item.manifestUrl}
                />
                <AppChips
                  items={dashboardPages.map(
                    (module) => `${module.icon} ${module.title}`
                  )}
                  dataCy="organization-apps-item-modules"
                />
                <AppChips
                  items={item.scopes}
                  variant="outlined"
                  dataCy="organization-apps-item-scopes"
                />
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
          organizationId={organization.id}
          install={refreshing}
          onClose={() => setRefreshing(null)}
        />
      )}
    </>
  );
};
