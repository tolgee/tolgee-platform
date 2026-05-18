import { useEffect, useMemo, useRef, useState } from 'react';
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
  border: none;
  background: transparent;
  display: block;
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
  const iframeRef = useRef<HTMLIFrameElement | null>(null);

  useEffect(() => {
    if (!app || !module) return;
    setToken(null);
    tokenMutation.mutate(
      { path: { projectId: project.id, installId } },
      { onSuccess: (data) => setToken(data.token) }
    );
  }, [installId, moduleKey, project.id, Boolean(app), Boolean(module)]);

  const iframeSrc =
    app && module && token ? `${app.baseUrl}${module.entry}` : null;

  const appOrigin = useMemo(() => {
    if (!app) return null;
    try {
      return new URL(app.baseUrl).origin;
    } catch {
      return null;
    }
  }, [app?.baseUrl]);

  const apiUrl = import.meta.env.VITE_APP_API_URL ?? window.location.origin;

  useEffect(() => {
    if (!appOrigin || !token) return;

    const handler = (event: MessageEvent) => {
      if (event.origin !== appOrigin) return;
      if (event.source !== iframeRef.current?.contentWindow) return;
      if (event.data?.type !== 'tolgee-app:ready') return;

      iframeRef.current?.contentWindow?.postMessage(
        {
          type: 'tolgee-app:init',
          token,
          projectId: project.id,
          apiUrl,
        },
        appOrigin
      );
    };

    window.addEventListener('message', handler);
    return () => window.removeEventListener('message', handler);
  }, [appOrigin, token, project.id, apiUrl]);

  return (
    <BaseProjectView
      maxWidth={1200}
      windowTitle={module?.title ?? 'App'}
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
          ref={iframeRef}
          data-cy="project-app-page-iframe"
          src={iframeSrc}
          // `allow-same-origin` is required so the plugin can load its own
          // assets (scripts, fetches) from its origin without CORS rejections.
          // Isolation from the Tolgee parent is enforced by the plugin running
          // on a different origin (per-plugin subdomain in prod, localhost:5180
          // vs localhost:3824 in dev) — NOT by null-origin sandboxing.
          sandbox="allow-scripts allow-forms allow-same-origin"
          title={module.title}
        />
      )}
    </BaseProjectView>
  );
};
