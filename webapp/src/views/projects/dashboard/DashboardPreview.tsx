import { BaseView } from 'tg.component/layout/BaseView';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import React from 'react';
import { useProject } from 'tg.hooks/useProject';
import { ProjectTotals } from './ProjectTotals';
import { Box, Grid, styled } from '@mui/material';
import { LanguageStats } from './LanguageStats';
import { DailyActivityChart } from './DailyActivityChart';

export const DashboardPreview = () => {
  const project = useProject();

  const statsLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/stats',
    method: 'get',
    path: {
      projectId: project.id,
    },
  });

  return (
    <BaseView loading={statsLoadable.isLoading}>
      {statsLoadable.data && (
        <StyledContainer>
          <Box>
            <ProjectTotals stats={statsLoadable.data} />
          </Box>
          <StyledLanguagesAndActivityGridContainer container>
            <Grid item xl={6}>
              <LanguageStats languageStats={statsLoadable.data.languageStats} />
            </Grid>
            <Grid item xl={6}>
              <Box display="flex" alignItems="center" justifyContent="center">
                Activity here
              </Box>
            </Grid>
          </StyledLanguagesAndActivityGridContainer>
          <DailyActivityChart />
        </StyledContainer>
      )}
    </BaseView>
  );
};

const StyledContainer = styled(Box)`
  display: flex;
  min-height: 100%;
  flex-direction: column;
`;

const StyledLanguagesAndActivityGridContainer = styled(Grid)`
  flex-grow: 1;
`;
