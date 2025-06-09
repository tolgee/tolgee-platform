import { Box, styled } from '@mui/material';
import { useTranslate } from '@tolgee/react';

import { useApiInfiniteQuery, useApiQuery } from 'tg.service/http/useQueryApi';
import { EmptyListMessage } from 'tg.component/common/EmptyListMessage';
import { useProject } from 'tg.hooks/useProject';
import { ProjectLanguagesProvider } from 'tg.hooks/ProjectLanguagesProvider';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';

import { ProjectTotals } from './ProjectTotals';
import { LanguageStats } from './LanguageStats/LanguageStats';
import { DailyActivityChart } from './DailyActivityChart';
import { ActivityList } from './ActivityList';
import { BaseProjectView } from '../BaseProjectView';
import { ProjectDescription } from './ProjectDescription';
import { useReportEvent } from 'tg.hooks/useReportEvent';
import { useEffect } from 'react';
import { ProjectNameAndId } from './ProjectNameAndId';
import { ProjectSettingsRight } from './ProjectSettingsRight';

const StyledContainer = styled(Box)`
  display: grid;
  grid-template:
    'title         title'
    'totalStats    totalStats'
    'langStats     activityList'
    'activityChart activityChart';
  grid-template-columns: 1fr 1fr;
  grid-template-rows: auto auto minmax(300px, auto) auto;
  min-height: 100%;
  flex-direction: column;
  gap: 16px 16px;
  padding-bottom: 60px;
  @container main-container (min-width: 1400px) {
    padding-top: 24px;
  }
  @container main-container (max-width: 1000px) {
    grid-template-columns: 1fr;
    grid-template-rows: auto auto auto auto;
    grid-template-areas:
      'title'
      'totalStats'
      'langStats'
      'activityList'
      'activityChart';
  }
`;

const StyledTopInfo = styled(Box)`
  display: flex;
  grid-area: title;
  justify-content: space-between;
`;

export const DashboardView = () => {
  const project = useProject();

  const reportEvent = useReportEvent();
  useEffect(() => {
    if (project?.name === 'Demo project') {
      reportEvent('DEMO_PROJECT_VIEWED');
    }
  }, [project?.name]);

  const { t } = useTranslate();
  const { satisfiesPermission } = useProjectPermissions();

  const canViewActivity = satisfiesPermission('activity.view');

  const path = { projectId: project.id };
  const query = { size: 15, sort: ['timestamp,desc'] };
  const activityLoadable = useApiInfiniteQuery({
    url: '/v2/projects/{projectId}/activity',
    method: 'get',
    path,
    query,
    options: {
      enabled: canViewActivity,
      getNextPageParam: (lastPage) => {
        if (
          lastPage.page &&
          lastPage.page.number! < lastPage.page.totalPages! - 1
        ) {
          return {
            path,
            query: {
              ...query,
              page: lastPage.page!.number! + 1,
            },
          };
        } else {
          return null;
        }
      },
    },
  });

  const statsLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/stats',
    method: 'get',
    path: {
      projectId: project.id,
    },
  });

  const dailyActivityLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/stats/daily-activity',
    method: 'get',
    path: {
      projectId: project.id,
    },
    options: {
      enabled: canViewActivity,
    },
  });

  const anythingLoading =
    activityLoadable.isLoading ||
    statsLoadable.isLoading ||
    dailyActivityLoadable.isLoading;

  return (
    <BaseProjectView windowTitle={t('project_dashboard_title')} maxWidth="wide">
      <ProjectLanguagesProvider>
        {anythingLoading ? (
          <EmptyListMessage loading={true} />
        ) : (
          <StyledContainer>
            <StyledTopInfo>
              <ProjectNameAndId project={project} />
              <ProjectSettingsRight project={project} />
            </StyledTopInfo>
            <Box gridArea="totalStats">
              <ProjectTotals stats={statsLoadable.data!} />
              {project.description && (
                <Box marginTop={2} display="grid">
                  <ProjectDescription description={project.description} />
                </Box>
              )}
            </Box>
            <Box gridArea="langStats">
              <LanguageStats
                languageStats={statsLoadable.data!.languageStats}
                wordCount={statsLoadable.data!.baseWordsCount}
              />
            </Box>
            {canViewActivity && (
              <>
                <Box
                  gridArea="activityList"
                  data-cy="project-dashboard-activity-list"
                >
                  <ActivityList activityLoadable={activityLoadable} />
                </Box>
                <Box
                  gridArea="activityChart"
                  data-cy="project-dashboard-activity-chart"
                >
                  <DailyActivityChart
                    dailyActivity={dailyActivityLoadable.data}
                  />
                </Box>
              </>
            )}
          </StyledContainer>
        )}
      </ProjectLanguagesProvider>
    </BaseProjectView>
  );
};
