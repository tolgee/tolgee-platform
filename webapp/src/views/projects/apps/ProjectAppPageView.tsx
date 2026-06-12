import { Box, CircularProgress, styled, Typography } from '@mui/material';
import { T } from '@tolgee/react';
import { useParams } from 'react-router-dom';

import { BaseProjectView } from '../BaseProjectView';
import { LINKS, PARAMS } from 'tg.constants/links';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';
import { useAppIframeMessaging } from '../translations/decorators/useAppIframeMessaging';

const StyledIframe = styled('iframe')`
  width: 100%;
  height: 100%;
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
  const installId = Number(params[PARAMS.APP_INSTALL_ID]);
  const moduleKey = params[PARAMS.APP_MODULE_KEY];

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

  // Full-page dashboard iframe — no per-cell selection, but it goes through the
  // shared messaging so it gets the context token, theme, and theme changes.
  const { iframeRef, iframeSrc } = useAppIframeMessaging({
    installId,
    projectId: project.id,
    organizationId: project.organizationOwner?.id ?? null,
    baseUrl: app?.baseUrl ?? '',
    entry: module?.entry ?? '',
    selection: {
      keyId: null,
      languageId: null,
      languageTag: null,
      translationId: null,
    },
  });

  return (
    <BaseProjectView
      maxWidth="max"
      stretch
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
          // allow-popups(+-to-escape-sandbox) lets in-app links / window.open open a
          // normal (non-sandboxed) new tab; allow-top-navigation-by-user-activation lets a
          // click navigate the whole Tolgee window (e.g. target="_top" links).
          sandbox="allow-scripts allow-forms allow-same-origin allow-popups allow-popups-to-escape-sandbox allow-top-navigation-by-user-activation"
          title={module.title}
        />
      )}
    </BaseProjectView>
  );
};
