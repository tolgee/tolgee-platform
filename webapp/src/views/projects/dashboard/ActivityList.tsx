import React, { useMemo } from 'react';
import { styled, Typography, FormControlLabel, Switch } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';

import { ActivityCompact } from 'tg.component/activity/ActivityCompact/ActivityCompact';
import { components } from 'tg.service/apiSchema.generated';
import { ActivityDateSeparator } from 'tg.views/projects/dashboard/ActivityDateSeparator';
import { useState } from 'react';
import { useDateCounter } from 'tg.hooks/useDateCounter';
import { useProject } from 'tg.hooks/useProject';
import { useApiInfiniteQuery } from 'tg.service/http/useQueryApi';
import { useGlobalLoading } from 'tg.component/GlobalLoading';
import LoadingButton from 'tg.component/common/form/LoadingButton';

type ProjectActivityModel = components['schemas']['ProjectActivityModel'];

const StyledContainer = styled('div')`
  display: grid;
  gap: 5px;
  border: 1px solid ${({ theme }) => theme.palette.divider1.main};
  border-radius: 10px;
`;

const StyledHeader = styled('div')`
  display: flex;
  justify-content: space-between;
  margin: 12px 18px 8px 12px;
`;

const StyledScroller = styled('div')`
  overflow-y: auto;
  overflow-x: hidden;
  max-height: 600px;
`;

const StyledList = styled('div')`
  display: grid;
  grid-template-columns: fit-content(30%) 1fr auto;
  gap: 3px 0px;
  padding-bottom: 12px;
`;

const StyledLoadingButton = styled(LoadingButton)`
  grid-column: 1 / span 3;
  justify-self: center;
  margin-top: 5px;
`;

export const ActivityList: React.FC = () => {
  const project = useProject();

  const path = { projectId: project.id };
  const query = { size: 15, sort: ['timestamp,desc'] };
  const activity = useApiInfiniteQuery({
    url: '/v2/projects/{projectId}/activity',
    method: 'get',
    path,
    query,
    options: {
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

  const data = useMemo(() => {
    const result: ProjectActivityModel[] = [];
    activity.data?.pages.forEach((p) =>
      p._embedded?.activities?.forEach((activity) => {
        result.push(activity);
      })
    );
    return result;
  }, [activity.data]);

  const t = useTranslate();
  const [diffEnabled, setDiffEnabled] = useState(true);

  const toggleDiff = () => {
    setDiffEnabled(!diffEnabled);
  };

  const counter = useDateCounter();

  useGlobalLoading(activity.isFetching);

  return (
    <StyledContainer>
      <StyledHeader>
        <Typography variant="h5">
          <T keyName="dashboard_activity_title" />
        </Typography>
        <FormControlLabel
          control={
            <Switch size="small" checked={diffEnabled} onChange={toggleDiff} />
          }
          label={t('dashboard_activity_differences')}
          labelPlacement="start"
        />
      </StyledHeader>
      <StyledScroller>
        <StyledList>
          {data?.map((item) => {
            const date = new Date(item.timestamp);
            return (
              <React.Fragment key={item.revisionId}>
                {counter.isNewDate(date) && (
                  <ActivityDateSeparator date={date} />
                )}
                <ActivityCompact data={item} diffEnabled={diffEnabled} />
              </React.Fragment>
            );
          })}
          {activity.hasNextPage && (
            <StyledLoadingButton
              onClick={() => activity.fetchNextPage()}
              loading={activity.isFetchingNextPage}
            >
              <T keyName="global_load_more" />
            </StyledLoadingButton>
          )}
        </StyledList>
      </StyledScroller>
    </StyledContainer>
  );
};
