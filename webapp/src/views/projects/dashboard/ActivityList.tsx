import React, { useEffect, useMemo } from 'react';
import { styled, Typography, Box, Button } from '@mui/material';
import { Link } from 'react-router-dom';
import { LINKS, PARAMS } from 'tg.constants/links';
import { useProject } from 'tg.hooks/useProject';
import { T } from '@tolgee/react';
import { UseInfiniteQueryResult } from 'react-query';
import { useInView } from 'react-intersection-observer';

import { components } from 'tg.service/apiSchema.generated';
import { ActivityDateSeparator } from 'tg.views/projects/dashboard/ActivityDateSeparator';
import { useState } from 'react';
import { useDateCounter } from 'tg.hooks/useDateCounter';
import { BoxLoading } from 'tg.component/common/BoxLoading';
import { ActivityRevisionRow } from 'tg.views/projects/activityStream/ActivityRevisionRow';
import { ActivityRevisionDialog } from 'tg.views/projects/activityStream/ActivityRevisionDialog';
import { ActivityShowPastTimesContext } from 'tg.views/projects/activityStream/ActivityRowShell';
import { ActivityMiniFilter } from 'tg.views/projects/activityStream/ActivityMiniFilter';
import {
  ActivityFilter,
  useActivityFilter,
} from 'tg.views/projects/activityStream/activityStreamQuery';

type ProjectActivityModel = components['schemas']['ProjectActivityModel'];
type PagedModelProjectActivityModel =
  components['schemas']['PagedModelProjectActivityModel'];

const StyledContainer = styled('div')`
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  border: 1px solid ${({ theme }) => theme.palette.divider1};
  border-radius: 10px;
  overflow: hidden;
  background: ${({ theme }) => theme.palette.background.paper};
`;

const StyledHeader = styled('div')`
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 10px 0px 22px;
`;

const StyledScroller = styled('div')`
  overflow-y: auto;
  overflow-x: hidden;
  flex: 1;
  min-height: 0;
  margin: 0 2px 10px 0;
  padding: 0 0 0 8px;
  &::-webkit-scrollbar {
    width: 6px;
  }
  &::-webkit-scrollbar-track {
    margin-top: 36px;
    margin-bottom: 12px;
    background: transparent;
  }
  &::-webkit-scrollbar-thumb {
    border-radius: 3px;
    background: transparent;
  }
  &:hover::-webkit-scrollbar-thumb {
    background: ${({ theme }) => theme.palette.divider1};
  }
  mask-image: linear-gradient(to bottom, #000 calc(100% - 24px), transparent);
`;

const StyledList = styled('div')`
  display: grid;
  grid-template-columns: fit-content(30%) 1fr auto;
  gap: 3px 0px;
  padding-bottom: 12px;
`;

const StyledLoadingWrapper = styled(Box)`
  grid-column: 1 / span 3;
  justify-self: center;
  margin-top: 5px;
`;

type Props = {
  activityLoadable: UseInfiniteQueryResult<PagedModelProjectActivityModel>;
  filter: ActivityFilter;
  onFilterChange: (value: ActivityFilter) => void;
  filterState: ReturnType<typeof useActivityFilter>;
};

export const ActivityList: React.FC<React.PropsWithChildren<Props>> = ({
  activityLoadable,
  filter,
  onFilterChange,
  filterState,
}) => {
  const { ref, inView } = useInView({ rootMargin: '100px' });
  const project = useProject();
  const [openedRevision, setOpenedRevision] = useState<ProjectActivityModel>();

  const data = useMemo(() => {
    const result: ProjectActivityModel[] = [];
    activityLoadable.data?.pages.forEach((p) =>
      p._embedded?.activities?.forEach((activity) => {
        result.push(activity);
      })
    );
    return result;
  }, [activityLoadable.data]);

  const counter = useDateCounter();

  useEffect(() => {
    if (inView) {
      activityLoadable.fetchNextPage();
    }
  }, [inView]);

  return (
    <StyledContainer>
      <StyledHeader>
        <Typography variant="h4">
          <T keyName="dashboard_activity_title" />
        </Typography>
        <Box display="flex" alignItems="center" gap={2}>
          <Button
            size="small"
            component={Link}
            to={LINKS.PROJECT_ACTIVITY.build({
              [PARAMS.PROJECT_ID]: project.id,
            })}
            data-cy="dashboard-activity-stream-link"
          >
            <T
              keyName="dashboard_activity_stream_link"
              defaultValue="Activity stream"
            />
          </Button>
          <ActivityMiniFilter
            value={filter}
            onChange={onFilterChange}
            filter={filterState}
          />
        </Box>
      </StyledHeader>
      <ActivityShowPastTimesContext.Provider value={true}>
        <StyledScroller>
          <StyledList>
            {data?.map((item) => {
              const date = new Date(item.timestamp);
              return (
                <React.Fragment key={item.revisionId}>
                  {counter.isNewDate(date) && (
                    <ActivityDateSeparator date={date} />
                  )}
                  <ActivityRevisionRow
                    data={item}
                    diffEnabled={false}
                    onOpen={() => setOpenedRevision(item)}
                  />
                </React.Fragment>
              );
            })}
            {activityLoadable.hasNextPage && (
              <StyledLoadingWrapper ref={ref}>
                <BoxLoading />
              </StyledLoadingWrapper>
            )}
          </StyledList>
        </StyledScroller>
      </ActivityShowPastTimesContext.Provider>
      {openedRevision && (
        <ActivityRevisionDialog
          data={openedRevision}
          onClose={() => setOpenedRevision(undefined)}
        />
      )}
    </StyledContainer>
  );
};
