import React, { useEffect, useMemo } from 'react';
import { styled, Typography, Box } from '@mui/material';
import { T } from '@tolgee/react';
import { UseInfiniteQueryResult } from 'react-query';
import { useInView } from 'react-intersection-observer';

import { components } from 'tg.service/apiSchema.generated';
import { ActivityGroupItem } from 'tg.component/activity/groups/ActivityGroupItem';
import { ActivityDateSeparator } from 'tg.views/projects/dashboard/ActivityDateSeparator';
import { useDateCounter } from 'tg.hooks/useDateCounter';
import { BoxLoading } from 'tg.component/common/BoxLoading';

type ActivityGroupModel = components['schemas']['ActivityGroupModel'];
type PagedModelActivityGroupModel =
  components['schemas']['PagedModelActivityGroupModel'];

const StyledContainer = styled('div')`
  display: grid;
  gap: 5px;
  border: 1px solid ${({ theme }) => theme.palette.divider1};
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
  gap: 8px;
  padding: 0px 8px 12px 8px;
`;

const StyledLoadingWrapper = styled(Box)`
  justify-self: center;
  margin-top: 5px;
`;

type Props = {
  activityLoadable: UseInfiniteQueryResult<PagedModelActivityGroupModel>;
};

export const ActivityGroupsList: React.FC<Props> = ({ activityLoadable }) => {
  const { ref, inView } = useInView({ rootMargin: '100px' });

  const data = useMemo(() => {
    const result: ActivityGroupModel[] = [];
    activityLoadable.data?.pages.forEach((p) =>
      p._embedded?.groups?.forEach((group) => {
        result.push(group);
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
      </StyledHeader>
      <StyledScroller>
        <StyledList>
          {data.map((item) => {
            const date = new Date(item.timestamp);
            return (
              <React.Fragment key={item.id}>
                {counter.isNewDate(date) && (
                  <ActivityDateSeparator date={date} />
                )}
                <ActivityGroupItem item={item} />
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
    </StyledContainer>
  );
};
