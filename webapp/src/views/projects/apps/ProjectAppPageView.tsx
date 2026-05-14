import { Box, styled, Typography } from '@mui/material';
import { T } from '@tolgee/react';
import { useParams } from 'react-router-dom';

import { BaseProjectView } from '../BaseProjectView';
import { LINKS, PARAMS } from 'tg.constants/links';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';

const StyledPlaceholder = styled('div')`
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
      <StyledPlaceholder data-cy="project-app-page-placeholder">
        <Typography variant="h6">
          {module?.icon}{' '}
          {module?.title ?? (
            <T
              keyName="project_app_page_unknown"
              defaultValue="Unknown app page"
            />
          )}
        </Typography>
        <Typography variant="body2" color="text.secondary">
          <T
            keyName="project_app_page_placeholder"
            defaultValue="The app iframe will render here. Sandbox loading lands with the next scope."
          />
        </Typography>
        {app && (
          <Box mt={1}>
            <Typography variant="caption" color="text.secondary">
              entry: {app.baseUrl}
              {module?.entry ?? ''}
            </Typography>
          </Box>
        )}
      </StyledPlaceholder>
    </BaseProjectView>
  );
};
