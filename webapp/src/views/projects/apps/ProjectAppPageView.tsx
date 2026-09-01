import { useEffect, useRef } from 'react';
import { Box, CircularProgress, styled, Typography } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { useParams } from 'react-router-dom';

import { BaseProjectView } from 'tg.views/projects/BaseProjectView';
import { LINKS, PARAMS } from 'tg.constants/links';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';
import { useReportEvent } from 'tg.hooks/useReportEvent';
import { useAppIframeMessaging } from 'tg.views/projects/apps/useAppIframeMessaging';

const LOADING_MIN_HEIGHT = 400;

const StyledIframe = styled('iframe')`
  width: 100%;
  height: 100%;
  border: none;
  background: transparent;
  display: block;
`;

const StyledMissing = styled('div')`
  display: grid;
  gap: ${({ theme }) => theme.spacing(1)};
  padding: ${({ theme }) => theme.spacing(3)};
  border: 1px dashed ${({ theme }) => theme.palette.divider};
  border-radius: ${({ theme }) => theme.shape.borderRadius}px;
`;

export const ProjectAppPageView = () => {
  const project = useProject();
  const { t } = useTranslate();
  const params = useParams<Record<string, string>>();
  const installId = Number(params[PARAMS.APP_INSTALL_ID]);
  const moduleKey = params[PARAMS.APP_MODULE_KEY];

  const apps = useApiQuery({
    url: '/v2/projects/{projectId}/apps/enabled',
    method: 'get',
    path: { projectId: project.id },
  });

  const app = apps.data?._embedded?.projectApps?.find(
    (item) => item.id === installId
  );
  const module = app?.modules?.['project-dashboard-page']?.find(
    (m) => m.key === moduleKey
  );

  // Reports opening an app's dashboard page to analytics (grouped by organization
  // in PostHog), once per app page — re-fired when the user opens a different app
  // or page, not on the iframe's hourly token refresh.
  const reportEvent = useReportEvent();
  const reportedKeyRef = useRef<string | null>(null);
  useEffect(() => {
    const key = `${installId}:${moduleKey}`;
    if (app && reportedKeyRef.current !== key) {
      reportedKeyRef.current = key;
      reportEvent('APP_OPENED', {
        appId: app.appId,
        appName: app.name,
        installId,
      });
    }
  }, [app, installId, moduleKey]);

  // Full-page dashboard iframe — it goes through the shared messaging so it
  // gets the context token, theme, and theme changes.
  const { iframeRef, iframeSrc } = useAppIframeMessaging({
    installId,
    projectId: project.id,
    organizationId: project.organizationOwner?.id ?? null,
    baseUrl: app?.baseUrl ?? '',
    entry: module?.entry ?? '',
  });

  const title = module?.title ?? t('project_app_page_fallback_title', 'App');

  const loadingPlaceholder = (
    <Box
      display="flex"
      justifyContent="center"
      alignItems="center"
      minHeight={LOADING_MIN_HEIGHT}
      data-cy="project-app-page-loading"
    >
      <CircularProgress />
    </Box>
  );

  return (
    <BaseProjectView
      maxWidth="max"
      stretch
      windowTitle={title}
      navigation={[
        [
          title,
          LINKS.PROJECT_APP_PAGE.build({
            [PARAMS.PROJECT_ID]: project.id,
            [PARAMS.APP_INSTALL_ID]: installId,
            [PARAMS.APP_MODULE_KEY]: moduleKey,
          }),
        ],
      ]}
    >
      {apps.isLoading ? (
        loadingPlaceholder
      ) : !app || !module ? (
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
              defaultValue="This app is no longer enabled for this project, or the requested page doesn't exist."
            />
          </Typography>
        </StyledMissing>
      ) : !iframeSrc ? (
        loadingPlaceholder
      ) : (
        <StyledIframe
          ref={iframeRef}
          data-cy="project-app-page-iframe"
          src={iframeSrc}
          // `allow-same-origin` is required so the app can load its own
          // assets (scripts, fetches) from its origin without CORS rejections.
          // Isolation from the Tolgee parent is enforced by the app running
          // on a different origin (per-app subdomain in prod, localhost:5180
          // vs localhost:3824 in dev) — NOT by null-origin sandboxing.
          // allow-popups(+-to-escape-sandbox) lets in-app links / window.open open a
          // normal (non-sandboxed) new tab; allow-top-navigation-by-user-activation lets a
          // click navigate the whole Tolgee window (e.g. target="_top" links).
          sandbox="allow-scripts allow-forms allow-same-origin allow-popups allow-popups-to-escape-sandbox allow-top-navigation-by-user-activation"
          title={title}
        />
      )}
    </BaseProjectView>
  );
};
