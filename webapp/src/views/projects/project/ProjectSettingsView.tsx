import { FunctionComponent } from 'react';
import { Box, Tab, Tabs, styled } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { Link, useRouteMatch } from 'react-router-dom';

import { LINKS, PARAMS } from 'tg.constants/links';
import { useProject } from 'tg.hooks/useProject';

import { BaseProjectView } from '../BaseProjectView';
import { ProjectSettingsGeneral } from './ProjectSettingsGeneral';
import { ProjectSettingsAdvanced } from './ProjectSettingsAdvanced';

const StyledTabs = styled(Tabs)`
  margin-bottom: -1px;
`;

const StyledTabWrapper = styled(Box)`
  border-bottom: 1px solid ${({ theme }) => theme.palette.divider1};
`;

export const ProjectSettingsView: FunctionComponent = () => {
  const project = useProject();
  const { t } = useTranslate();

  const pageGeneral = useRouteMatch(LINKS.PROJECT_EDIT.template);
  const pageAdvanced = useRouteMatch(LINKS.PROJECT_EDIT_ADVANCED.template);

  return (
    <BaseProjectView
      maxWidth={800}
      windowTitle={t('project_settings_title')}
      title={t('project_settings_title')}
      navigation={[
        [
          t('project_settings_title'),
          LINKS.PROJECT_EDIT.build({
            [PARAMS.PROJECT_ID]: project.id,
          }),
        ],
      ]}
    >
      <StyledTabWrapper>
        <StyledTabs
          value={
            pageGeneral?.isExact
              ? 'general'
              : pageAdvanced?.isExact
              ? 'advanced'
              : null
          }
        >
          <Tab
            value="general"
            component={Link}
            to={LINKS.PROJECT_EDIT.build({
              [PARAMS.PROJECT_ID]: project.id,
            })}
            label={t('project_settings_menu_general')}
            data-cy="project-settings-menu-general"
          />
          <Tab
            value="advanced"
            component={Link}
            to={LINKS.PROJECT_EDIT_ADVANCED.build({
              [PARAMS.PROJECT_ID]: project.id,
            })}
            label={t('project_settings_menu_advanced')}
            data-cy="project-settings-menu-advanced"
          />
        </StyledTabs>
      </StyledTabWrapper>

      <Box data-cy="project-settings">
        {pageGeneral?.isExact && <ProjectSettingsGeneral />}
        {pageAdvanced?.isExact && <ProjectSettingsAdvanced />}
      </Box>
    </BaseProjectView>
  );
};
