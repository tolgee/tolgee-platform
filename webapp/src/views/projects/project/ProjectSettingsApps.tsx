import { Box, styled, Switch, Typography } from '@mui/material';
import { T } from '@tolgee/react';

import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';
import { AppSummary } from 'tg.component/apps/AppSummary';

const StyledContainer = styled('div')`
  display: grid;
  gap: ${({ theme }) => theme.spacing(2)};
  padding: ${({ theme }) => theme.spacing(2, 0)};
`;

const StyledRow = styled('div')`
  display: flex;
  justify-content: space-between;
  align-items: center;
  border: 1px solid ${({ theme }) => theme.palette.divider};
  border-radius: ${({ theme }) => theme.shape.borderRadius}px;
  padding: ${({ theme }) => theme.spacing(1.5, 2)};
  gap: ${({ theme }) => theme.spacing(2)};
`;

const StyledMeta = styled('div')`
  display: grid;
  gap: ${({ theme }) => theme.spacing(0.25)};
  min-width: 0;
`;

const StyledEmpty = styled('div')`
  padding: ${({ theme }) => theme.spacing(2)};
  color: ${({ theme }) => theme.palette.text.secondary};
`;

export const ProjectSettingsApps = () => {
  const project = useProject();

  const apps = useApiQuery({
    url: '/v2/projects/{projectId}/apps',
    method: 'get',
    path: { projectId: project.id },
  });

  const enableMutation = useApiMutation({
    url: '/v2/projects/{projectId}/apps/{installId}',
    method: 'put',
    invalidatePrefix: '/v2/projects/{projectId}/apps',
  });

  const disableMutation = useApiMutation({
    url: '/v2/projects/{projectId}/apps/{installId}',
    method: 'delete',
    invalidatePrefix: '/v2/projects/{projectId}/apps',
  });

  const toggle = (installId: number, enable: boolean) => {
    const path = { projectId: project.id, installId };
    if (enable) {
      enableMutation.mutate({ path });
    } else {
      disableMutation.mutate({ path });
    }
  };

  const items = apps.data?._embedded?.projectApps ?? [];

  return (
    <StyledContainer data-cy="project-settings-apps">
      <Box>
        <Typography variant="h6">
          <T keyName="project_settings_apps_title" defaultValue="Apps" />
        </Typography>
        <Typography variant="body2" color="text.secondary">
          <T
            keyName="project_settings_apps_description"
            defaultValue="Enable apps registered for this organization to make their pages available in the project sidebar."
          />
        </Typography>
      </Box>

      {items.length === 0 && (
        <StyledEmpty data-cy="project-settings-apps-empty">
          <T
            keyName="project_settings_apps_empty"
            defaultValue="No apps registered for this organization yet."
          />
        </StyledEmpty>
      )}

      {items.map((item) => (
        <StyledRow key={item.id} data-cy="project-settings-apps-item">
          <StyledMeta>
            <AppSummary
              name={item.name}
              version={item.version}
              url={item.baseUrl}
            />
          </StyledMeta>
          <Switch
            data-cy="project-settings-apps-item-toggle"
            checked={item.enabled}
            onChange={(_, checked) => toggle(item.id, checked)}
            disabled={enableMutation.isLoading || disableMutation.isLoading}
          />
        </StyledRow>
      ))}
    </StyledContainer>
  );
};
