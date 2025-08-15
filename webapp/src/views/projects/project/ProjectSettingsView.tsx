import { FunctionComponent } from 'react';
import { Box, Tab, Tabs, styled } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { Link, match, Redirect, useRouteMatch } from 'react-router-dom';

import { LINKS, PARAMS } from 'tg.constants/links';
import { useProject } from 'tg.hooks/useProject';

import { BaseProjectView } from '../BaseProjectView';
import { ProjectSettingsGeneral } from './ProjectSettingsGeneral';
import { ProjectSettingsAdvanced } from './ProjectSettingsAdvanced';
import { useAddProjectSettingsTabs } from 'tg.ee';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';
import { useReportEvent } from 'tg.hooks/useReportEvent';

export type ProjectSettingsTab = {
  value: string;
  label: string;
  link: string;
  dataCy?: string;
  component?: FunctionComponent;
  enabled: boolean;
  routeMatch: match | null;
};

const StyledTabs = styled(Tabs)`
  margin-bottom: -1px;
`;

const StyledTabWrapper = styled(Box)`
  border-bottom: 1px solid ${({ theme }) => theme.palette.divider1};
`;

export const ProjectSettingsView = () => {
  const project = useProject();
  const { t } = useTranslate();
  const { satisfiesPermission } = useProjectPermissions();
  const reportEvent = useReportEvent();

  let tabs = [
    {
      value: 'general',
      label: t('project_settings_menu_general'),
      link: LINKS.PROJECT_EDIT.build({
        [PARAMS.PROJECT_ID]: project.id,
      }),
      dataCy: 'project-settings-menu-general',
      component: ProjectSettingsGeneral,
      enabled: satisfiesPermission('project.edit'),
      routeMatch: useRouteMatch(LINKS.PROJECT_EDIT.template),
    },
    {
      value: 'advanced',
      label: t('project_settings_menu_advanced'),
      link: LINKS.PROJECT_EDIT_ADVANCED.build({
        [PARAMS.PROJECT_ID]: project.id,
      }),
      dataCy: 'project-settings-menu-advanced',
      component: ProjectSettingsAdvanced,
      enabled: satisfiesPermission('project.edit'),
      routeMatch: useRouteMatch(LINKS.PROJECT_EDIT_ADVANCED.template),
    },
  ] as ProjectSettingsTab[];

  tabs = useAddProjectSettingsTabs(project.id)(tabs);

  const matchedTab = tabs.find((t) => t.routeMatch?.isExact);
  if (matchedTab && !matchedTab.enabled) {
    const firstEnabled = tabs.find((t) => t.enabled);
    if (firstEnabled) {
      return <Redirect to={firstEnabled?.link} />;
    }
  }
  const ComponentToRender = matchedTab?.component;

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
        <StyledTabs value={matchedTab?.value}>
          {tabs
            .filter((tab) => tab.enabled)
            .map((tab) => (
              <Tab
                key={tab.value}
                value={tab.value}
                label={tab.label}
                component={Link}
                to={tab.link}
                data-cy={tab.dataCy}
                onClick={() => {
                  reportEvent('PROJECT_SETTINGS_TAB_CLICK', {
                    value: tab.value,
                  });
                }}
              />
            ))}
        </StyledTabs>
      </StyledTabWrapper>

      <Box data-cy="project-settings">
        {ComponentToRender && <ComponentToRender />}
      </Box>
    </BaseProjectView>
  );
};
