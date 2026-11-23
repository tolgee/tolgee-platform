import React, { useEffect, useMemo, useState } from 'react';
import { Box, styled, Typography } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { useInView } from 'react-intersection-observer';
import { LINKS, PARAMS } from 'tg.constants/links';
import { useProject } from 'tg.hooks/useProject';
import { useGlobalContext } from 'tg.globalContext/GlobalContext';
import { useUrlSearchState } from 'tg.hooks/useUrlSearchState';
import { useApiInfiniteQuery, useApiQuery } from 'tg.service/http/useQueryApi';
import { BaseProjectView } from 'tg.views/projects/BaseProjectView';
import { ProjectLanguagesProvider } from 'tg.hooks/ProjectLanguagesProvider';
import { BoxLoading } from 'tg.component/common/BoxLoading';
import { EmptyListMessage } from 'tg.component/common/EmptyListMessage';
import { useDateCounter } from 'tg.hooks/useDateCounter';
import { ActivityDateSeparator } from 'tg.views/projects/dashboard/ActivityDateSeparator';
import { ProjectActivityModel } from 'tg.component/activity/types';
import { LanguagesSelect } from 'tg.component/common/form/LanguagesSelect/LanguagesSelect';
import { ActivityRevisionRow } from './ActivityRevisionRow';
import { ActivityMultiSelect } from './ActivityMultiSelect';
import { ActivityTreeSelect } from './ActivityTreeSelect';
import { ActivityRevisionChanges } from './ActivityRevisionDialog';
import { ActivityShowPastTimesContext } from './ActivityRowShell';
import { useActivityFilter } from './activityStreamQuery';
import {
  ActivityDateRangeFilter,
  decodeDateRange,
  encodeDateRange,
  resolveDateRange,
} from './ActivityDateRangeFilter';

const StyledHeader = styled(Box)`
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 16px;
  position: sticky;
  z-index: 2;
  transition: top 0.2s;
  padding: 12px 0 8px;
  margin: -12px 0 0;
  background: ${({ theme }) => theme.palette.background.default};
`;

const StyledFilters = styled(Box)`
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
`;

const StyledList = styled(Box)`
  display: grid;
  grid-template-columns: fit-content(30%) 1fr auto;
  gap: 3px 0px;
`;

const StyledExpanded = styled(Box)`
  grid-column: 1 / span 3;
  margin: -4px 12px 12px 44px;
`;

export const ActivityStreamView = () => {
  const { t } = useTranslate();
  const project = useProject();
  const topBarHeight = useGlobalContext((c) => c.layout.topBarHeight);
  const [headerEl, setHeaderEl] = useState<HTMLDivElement | null>(null);
  const [headerHeight, setHeaderHeight] = useState(0);
  useEffect(() => {
    if (!headerEl) return;
    const observer = new ResizeObserver(() =>
      setHeaderHeight(headerEl.offsetHeight)
    );
    observer.observe(headerEl);
    return () => observer.disconnect();
  }, [headerEl]);
  const [users, setUsers] = useUrlSearchState('user', {
    array: true,
    defaultVal: [],
  });
  const [categoriesParam, setCategories] = useUrlSearchState('activity', {
    array: true,
    defaultVal: [],
  });
  const [languageTags, setLanguageTags] = useUrlSearchState('language', {
    array: true,
    defaultVal: [],
  });
  const [time, setTime] = useUrlSearchState('time', { defaultVal: 'ALL' });
  const [expanded, setExpanded] = useState<number>();
  const { ref, inView } = useInView({ rootMargin: '200px' });

  const filter = useActivityFilter(
    {
      users: users as string[],
      activities: categoriesParam as string[],
      languages: languageTags as string[],
    },
    (f) => {
      setUsers(f.users);
      setCategories(f.activities);
      setLanguageTags(f.languages);
    }
  );

  const bounds = useApiQuery({
    url: '/v2/projects/{projectId}/activity/stream/bounds',
    method: 'get',
    path: { projectId: project.id },
  });

  const dateRange = decodeDateRange(time as string | undefined);
  const { from, to } = resolveDateRange(dateRange);

  const path = { projectId: project.id };
  const query = {
    size: 30,
    sort: ['timestamp,desc'],
    ...filter.query,
    from,
    to,
  };

  const loadable = useApiInfiniteQuery({
    url: '/v2/projects/{projectId}/activity/stream',
    method: 'get',
    path,
    query,
    options: {
      keepPreviousData: true,
      getNextPageParam: (lastPage) => {
        const page = lastPage.page;
        if (page && page.number! + 1 < page.totalPages!) {
          return { path, query: { ...query, page: page.number! + 1 } };
        }
        return null;
      },
    },
  });

  React.useEffect(() => {
    if (inView && loadable.hasNextPage && !loadable.isFetchingNextPage) {
      loadable.fetchNextPage();
    }
  }, [inView, loadable.data?.pages.length, loadable.isFetchingNextPage]);

  const items = useMemo(
    () =>
      (loadable.data?.pages.flatMap((p) => p._embedded?.activities ?? []) ??
        []) as ProjectActivityModel[],
    [loadable.data]
  );
  const counter = useDateCounter();

  return (
    <BaseProjectView
      windowTitle={t('activity_stream_title', 'Activity stream')}
      maxWidth="wide"
      navigation={[
        [
          t('activity_stream_title', 'Activity stream'),
          LINKS.PROJECT_ACTIVITY.build({ [PARAMS.PROJECT_ID]: project.id }),
        ],
      ]}
    >
      <ProjectLanguagesProvider>
        <StyledHeader ref={setHeaderEl} sx={{ top: topBarHeight }}>
          <Typography variant="h4">
            <T keyName="activity_stream_title" defaultValue="Activity stream" />
          </Typography>
          <StyledFilters>
            <ActivityMultiSelect
              data-cy="activity-stream-filter-user"
              {...filter.users}
            />
            <ActivityTreeSelect
              data-cy="activity-stream-filter-activity"
              {...filter.activities}
            />
            <LanguagesSelect
              context="activity-stream"
              placeholder={t('activity_stream_no_languages', 'No languages')}
              {...filter.languages}
              enableEmpty
              selectAllToggle
            />
            <ActivityDateRangeFilter
              value={dateRange}
              onChange={(value) => setTime(encodeDateRange(value))}
              since={bounds.data?.since}
              maxRangeDays={bounds.data?.maxRangeDays}
            />
          </StyledFilters>
        </StyledHeader>
        {loadable.isLoading ? (
          <BoxLoading />
        ) : items.length === 0 ? (
          <EmptyListMessage>
            <T
              keyName="activity_stream_empty"
              defaultValue="No activity matches these filters"
            />
          </EmptyListMessage>
        ) : (
          <ActivityShowPastTimesContext.Provider value={true}>
            <StyledList data-cy="activity-stream-list">
              {items.map((item) => {
                const date = new Date(item.timestamp);
                const isExpanded = expanded === item.revisionId;
                return (
                  <React.Fragment key={item.revisionId}>
                    {counter.isNewDate(date) && (
                      <ActivityDateSeparator
                        date={date}
                        stickyTop={topBarHeight + headerHeight}
                        background="default"
                      />
                    )}
                    <ActivityRevisionRow
                      data={item}
                      diffEnabled={false}
                      expanded={isExpanded}
                      onOpen={() =>
                        setExpanded(isExpanded ? undefined : item.revisionId)
                      }
                    />
                    {isExpanded && (
                      <StyledExpanded>
                        <ActivityRevisionChanges data={item} />
                      </StyledExpanded>
                    )}
                  </React.Fragment>
                );
              })}
              {loadable.hasNextPage && (
                <Box gridColumn="1 / span 3" ref={ref}>
                  <BoxLoading />
                </Box>
              )}
            </StyledList>
          </ActivityShowPastTimesContext.Provider>
        )}
      </ProjectLanguagesProvider>
    </BaseProjectView>
  );
};
