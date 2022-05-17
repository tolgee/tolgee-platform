import { Box, Chip, styled } from '@mui/material';
import { T } from '@tolgee/react';

import { BaseView } from 'tg.component/layout/BaseView';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';
import { ProjectLanguagesProvider } from 'tg.hooks/ProjectLanguagesProvider';
import { ProjectTotals } from './ProjectTotals';
import { LanguageStats } from './LanguageStats/LanguageStats';
import { DailyActivityChart } from './DailyActivityChart';
import { ActivityList } from './ActivityList';
import { SecondaryBar } from 'tg.component/layout/SecondaryBar';
import { SmallProjectAvatar } from 'tg.component/navigation/SmallProjectAvatar';

const StyledContainer = styled(Box)`
  display: grid;
  grid-template:
    'totalStats    totalStats'
    'langStats     activityList'
    'activityChart activityChart';
  grid-template-columns: 1fr 1fr;
  grid-template-rows: auto minmax(300px, auto) auto;
  min-height: 100%;
  flex-direction: column;
  gap: 16px 16px;
  padding-bottom: 60px;

  @media (max-width: 1000px) {
    grid-template-columns: 1fr;
    grid-template-rows: auto auto auto auto;
    grid-template-areas:
      'totalStats'
      'langStats'
      'activityList'
      'activityChart';
  }
`;

const StyledHeader = styled(Box)`
  display: grid;
  grid-template-columns: auto auto auto 1fr auto;
  gap: 8px;
  align-items: center;
  margin-top: -4px;
  margin-bottom: -4px;
`;

const StyledProjectIcon = styled(Box)``;

const StyledProjectName = styled(Box)`
  margin-right: 4px;
  font-size: 16px;
`;

const StyledProjectId = styled(Box)`
  font-size: 14px;
  color: ${({ theme }) => theme.palette.text.secondary};
  grid-column: -1;
`;

export const DashboardView = () => {
  const project = useProject();

  const statsLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/stats',
    method: 'get',
    path: {
      projectId: project.id,
    },
  });

  return (
    <ProjectLanguagesProvider>
      <BaseView
        containerMaxWidth="xl"
        customNavigation={
          <SecondaryBar>
            <StyledHeader>
              <StyledProjectIcon>
                <SmallProjectAvatar project={project} />
              </StyledProjectIcon>
              <StyledProjectName>{project.name}</StyledProjectName>
              <Chip
                size="small"
                label={
                  project.userOwner?.name || project.organizationOwner?.name
                }
              />
              <StyledProjectId>
                <T
                  keyName="project_dashboard_project_id"
                  parameters={{ id: project.id }}
                />
              </StyledProjectId>
            </StyledHeader>
          </SecondaryBar>
        }
      >
        {statsLoadable.data && (
          <StyledContainer>
            <Box gridArea="totalStats">
              <ProjectTotals stats={statsLoadable.data} />
            </Box>
            <Box gridArea="langStats">
              <LanguageStats
                languageStats={statsLoadable.data.languageStats}
                wordCount={statsLoadable.data.baseWordsCount}
              />
            </Box>
            <Box gridArea="activityList">
              <ActivityList />
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
