import { useEffect, useState } from 'react';
import { Box, CircularProgress, styled, Typography } from '@mui/material';
import { T } from '@tolgee/react';
import { useParams } from 'react-router-dom';

import { BaseProjectView } from '../BaseProjectView';
import { LINKS, PARAMS } from 'tg.constants/links';
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';

const StyledIframe = styled('iframe')`
  width: 100%;
  min-height: 600px;
  border: 1px solid ${({ theme }) => theme.palette.divider};
  border-radius: 4px;
  background: ${({ theme }) => theme.palette.background.paper};
`;

const StyledMissing = styled('div')`
  display: grid;
  gap: 8px;
  padding: 24px;
  border: 1px dashed ${({ theme }) => theme.palette.divider};
  border-radius: 4px;
`;

export const ProjectAppPageView = () => {
  const project = useProject();
  const params = useParams<Record<string, string>>();
  const installIdParam = params[PARAMS.APP_INSTALL_ID];
  const moduleKey = params[PARAMS.APP_MODULE_KEY];
  const installId = Number(installIdParam);

  const apps = useApiQuery({
    url: '/v2/projects/{projectId}/apps',
    method: 'get',
    path: { projectId: project.id },
  });

  const app = apps.data?._embedded?.projectApps?.find(
    (item) => item.id === installId
  );
  const module = app?.modules?.['project-dashboard-page']?.find(
    (m) => m.key === moduleKey
  );

  const tokenMutation = useApiMutation({
    url: '/v2/projects/{projectId}/apps/{installId}/token',
    method: 'post',
  });

  const [token, setToken] = useState<string | null>(null);

  useEffect(() => {
    if (!app || !module) return;
    setToken(null);
    tokenMutation.mutate(
      { path: { projectId: project.id, installId } },
      { onSuccess: (data) => setToken(data.token) }
    );
  }, [installId, moduleKey, project.id, Boolean(app), Boolean(module)]);

  const iframeSrc =
    app && module && token
      ? `${app.baseUrl}${module.entry}?token=${encodeURIComponent(
          token
        )}&projectId=${project.id}&apiUrl=${encodeURIComponent(
          import.meta.env.VITE_APP_API_URL ?? window.location.origin
        )}`
      : null;

  return (
    <BaseProjectView
      maxWidth={1200}
      windowTitle={module?.title ?? 'App'}
      title={module?.title ?? 'App'}
      navigation={[
        [
          'Apps',
          LINKS.PROJECT_EDIT_APPS.build({ [PARAMS.PROJECT_ID]: project.id }),
        ],
        [
          module?.title ?? 'App',
          LINKS.PROJECT_APP_PAGE.build({
            [PARAMS.PROJECT_ID]: project.id,
            [PARAMS.APP_INSTALL_ID]: installId,
            [PARAMS.APP_MODULE_KEY]: moduleKey,
          }),
        ],
      ]}
    >
      {!app || !module ? (
        <StyledMissing data-cy="project-app-page-missing">
          <Typography variant="h6">
            <T
              keyName="project_app_page_unknown"
              defaultValue="Unknown app page"
            />
          </Typography>
          <Typography variant="body2" color="text.secondary">
            <T
              keyName="project_app_page_unknown_description"
              defaultValue="This plugin is no longer enabled for this project, or the requested page doesn't exist."
            />
          </Typography>
        </StyledMissing>
      ) : !iframeSrc ? (
        <Box
          display="flex"
          justifyContent="center"
          alignItems="center"
          minHeight={400}
          data-cy="project-app-page-loading"
        >
          <CircularProgress />
        </Box>
      ) : (
        <StyledIframe
          data-cy="project-app-page-iframe"
          src={iframeSrc}
          sandbox="allow-scripts allow-forms"
          title={module.title}
        />
      )}
    </BaseProjectView>
  );
};
