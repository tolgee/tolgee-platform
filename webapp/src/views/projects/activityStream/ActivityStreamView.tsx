import React, { useMemo, useState } from 'react';
import {
  Box,
  Divider,
  MenuItem,
  Select,
  styled,
  Typography,
} from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { useInView } from 'react-intersection-observer';
import { LINKS, PARAMS } from 'tg.constants/links';
import { useProject } from 'tg.hooks/useProject';
import { useUser } from 'tg.globalContext/helpers';
import { useUrlSearchState } from 'tg.hooks/useUrlSearchState';
import { useApiInfiniteQuery, useApiQuery } from 'tg.service/http/useQueryApi';
import { BaseProjectView } from 'tg.views/projects/BaseProjectView';
import { ProjectLanguagesProvider } from 'tg.hooks/ProjectLanguagesProvider';
import { BoxLoading } from 'tg.component/common/BoxLoading';
import { EmptyListMessage } from 'tg.component/common/EmptyListMessage';
import { useDateCounter } from 'tg.hooks/useDateCounter';
import { ActivityDateSeparator } from 'tg.views/projects/dashboard/ActivityDateSeparator';
import { ProjectActivityModel } from 'tg.component/activity/types';
import { ActivityRevisionRow } from './ActivityRevisionRow';
import { ActivityRevisionChanges } from './ActivityRevisionDialog';
import { ActivityShowPastTimesContext } from './ActivityRowShell';
import { ACTIVITY_CATEGORIES, ActivityCategory } from './activityCategories';
import { useActivityCategoryLabels } from './useActivityCategoryLabels';
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
  margin-bottom: 24px;
`;

const StyledFilters = styled(Box)`
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
`;

const StyledSelect = styled(Select)`
  min-width: 160px;
  font-size: 14px;
  & .MuiSelect-select {
    padding-top: 7px;
    padding-bottom: 7px;
  }
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
  const user = useUser();
  const [userFilter, setUserFilter] = useUrlSearchState('user', {
    defaultVal: 'ALL',
  });
  const [categoryParam, setCategory] = useUrlSearchState('activity', {
    defaultVal: 'ALL',
  });
  const category = categoryParam as ActivityCategory | 'ALL';
  const [languageId, setLanguageId] = useUrlSearchState('language', {
    defaultVal: 'ALL',
  });
  const [time, setTime] = useUrlSearchState('time', { defaultVal: 'ALL' });
  const [expanded, setExpanded] = useState<number>();
  const { ref, inView } = useInView({ rootMargin: '200px' });

  const members = useApiQuery({
    url: '/v2/projects/{projectId}/users',
    method: 'get',
    path: { projectId: project.id },
    query: { size: 1000 },
  });
  const languages = useApiQuery({
    url: '/v2/projects/{projectId}/languages',
    method: 'get',
    path: { projectId: project.id },
    query: { size: 1000 },
  });

  const dateRange = decodeDateRange(time as string | undefined);
  const { from, to } = resolveDateRange(dateRange);

  const path = { projectId: project.id };
  const query = {
    size: 30,
    sort: ['timestamp,desc'],
    authorId:
      userFilter !== 'ALL' && userFilter !== 'EXCEPT_ME'
        ? Number(userFilter)
        : undefined,
    excludeAuthorId: userFilter === 'EXCEPT_ME' ? user?.id : undefined,
    types:
      category === 'ALL'
        ? undefined
        : ([...ACTIVITY_CATEGORIES[category]] as string[]),
    languageIds: languageId === 'ALL' ? undefined : [Number(languageId)],
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

  const categoryLabels = useActivityCategoryLabels();

  return (
    <BaseProjectView
      windowTitle={t('activity_stream_title', 'Activity stream')}
      maxWidth={1000}
      navigation={[
        [
          t('activity_stream_title', 'Activity stream'),
          LINKS.PROJECT_ACTIVITY.build({ [PARAMS.PROJECT_ID]: project.id }),
        ],
      ]}
    >
      <ProjectLanguagesProvider>
        <StyledHeader>
          <Typography variant="h4">
            <T keyName="activity_stream_title" defaultValue="Activity stream" />
          </Typography>
          <StyledFilters>
            <StyledSelect
              size="small"
              value={userFilter}
              onChange={(e) => setUserFilter(e.target.value as string)}
              data-cy="activity-stream-filter-user"
            >
              <MenuItem value="ALL">
                <T
                  keyName="activity_stream_all_users"
                  defaultValue="All users"
                />
              </MenuItem>
              <MenuItem value="EXCEPT_ME">
                <T
                  keyName="activity_stream_except_me"
                  defaultValue="Everyone except me"
                />
              </MenuItem>
              <Divider />
              {members.data?._embedded?.users?.map((member) => (
                <MenuItem key={member.id} value={String(member.id)}>
                  {member.name}
                </MenuItem>
              ))}
            </StyledSelect>
            <StyledSelect
              size="small"
              value={category}
              onChange={(e) => setCategory(e.target.value as ActivityCategory)}
              data-cy="activity-stream-filter-activity"
            >
              <MenuItem value="ALL">
                <T
                  keyName="activity_stream_all_activities"
                  defaultValue="All activities"
                />
              </MenuItem>
              {(Object.keys(categoryLabels) as ActivityCategory[]).map((c) => (
                <MenuItem key={c} value={c}>
                  {categoryLabels[c]}
                </MenuItem>
              ))}
            </StyledSelect>
            <StyledSelect
              size="small"
              value={languageId}
              onChange={(e) => setLanguageId(e.target.value as string)}
              data-cy="activity-stream-filter-language"
            >
              <MenuItem value="ALL">
                <T
                  keyName="activity_stream_all_languages"
                  defaultValue="All languages"
                />
              </MenuItem>
              {languages.data?._embedded?.languages?.map((language) => (
                <MenuItem key={language.id} value={String(language.id)}>
                  {language.flagEmoji} {language.name}
                </MenuItem>
              ))}
            </StyledSelect>
            <ActivityDateRangeFilter
              value={dateRange}
              onChange={(value) => setTime(encodeDateRange(value))}
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
                      <ActivityDateSeparator date={date} />
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
