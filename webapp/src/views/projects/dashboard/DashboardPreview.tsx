import { BaseView } from 'tg.component/layout/BaseView';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';
import { ProjectTotals } from './ProjectTotals';
import { Box, styled } from '@mui/material';
import { LanguageStats } from './LanguageStats/LanguageStats';
import { DailyActivityChart } from './DailyActivityChart';
import { ActivityList } from './Activity/ActivityList';
import { ProjectLanguagesProvider } from 'tg.hooks/ProjectLanguagesProvider';

const StyledContainer = styled(Box)`
  display: grid;
  grid-template:
    'totalStats    totalStats'
    'langStats     activityList'
    'activityChart activityChart';
  grid-template-columns: 1fr 1fr;
  min-height: 100%;
  flex-direction: column;
  gap: 16px 16px;
`;

export const DashboardPreview = () => {
  const project = useProject();

  const statsLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/stats',
    method: 'get',
    path: {
      projectId: project.id,
    },
  });

  const activityLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/activity',
    method: 'get',
    path: { projectId: project.id },
    query: { size: 5, sort: ['timestamp,desc'] },
  });

  return (
    <ProjectLanguagesProvider>
      <BaseView
        loading={statsLoadable.isLoading || activityLoadable.isLoading}
        containerMaxWidth="lg"
      >
        {statsLoadable.data && (
          <StyledContainer>
            <Box gridArea="totalStats">
              <ProjectTotals stats={statsLoadable.data} />
            </Box>
            <Box gridArea="langStats" sx={{ ml: 2, mt: 2 }}>
              <LanguageStats
                languageStats={statsLoadable.data.languageStats}
                keyCount={statsLoadable.data.keyCount}
              />
            </Box>
            <Box gridArea="activityList">
              <ActivityList
                data={activityLoadable.data?._embedded?.activities}
              />
            </Box>
            <Box gridArea="activityChart">
              <DailyActivityChart />
            </Box>
          </StyledContainer>
        )}
      </BaseView>
    </ProjectLanguagesProvider>
  );
};
