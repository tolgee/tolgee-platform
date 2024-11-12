import React, { useEffect, useMemo } from 'react';
import {
  styled,
  Typography,
  FormControlLabel,
  Switch,
  Box,
} from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { UseInfiniteQueryResult } from 'react-query';
import { useInView } from 'react-intersection-observer';

import { ActivityCompact } from 'tg.component/activity/ActivityCompact/ActivityCompact';
import { components } from 'tg.service/apiSchema.generated';
import { ActivityDateSeparator } from 'tg.views/projects/dashboard/ActivityDateSeparator';
import { useState } from 'react';
import { useDateCounter } from 'tg.hooks/useDateCounter';
import { BoxLoading } from 'tg.component/common/BoxLoading';
import { ActivityDetailDialog } from 'tg.component/activity/ActivityDetail/ActivityDetailDialog';
import { ActivityModel } from 'tg.component/activity/types';
import { useUrlSearchState } from 'tg.hooks/useUrlSearchState';

type ProjectActivityModel = components['schemas']['ProjectActivityModel'];
type PagedModelProjectActivityModel =
  components['schemas']['PagedModelProjectActivityModel'];

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
};

export const ActivityList: React.FC<Props> = ({ activityLoadable }) => {
  const { ref, inView } = useInView({ rootMargin: '100px' });
  const [detailData, setDetailData] = useState<ActivityModel>();
  const [detailId, setDetailId] = useUrlSearchState('activity');

  const data = useMemo(() => {
    const result: ProjectActivityModel[] = [];
    activityLoadable.data?.pages.forEach((p) =>
      p._embedded?.activities?.forEach((activity) => {
        result.push(activity);
      })
    );
    return result;
  }, [activityLoadable.data]);

  const { t } = useTranslate();
  const [diffEnabled, setDiffEnabled] = useState(true);

  const toggleDiff = () => {
    setDiffEnabled(!diffEnabled);
  };

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
                <ActivityCompact
                  data={item}
                  diffEnabled={diffEnabled}
                  onDetailOpen={(detailData) => {
                    setDetailData(detailData);
                    setDetailId(String(detailData.revisionId));
                  }}
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
      {detailId && (
        <ActivityDetailDialog
          data={detailData}
          detailId={Number(detailId)}
          initialDiffEnabled={diffEnabled}
          open={Boolean(detailId)}
          onClose={() => {
            setDetailData(undefined);
            setDetailId(undefined);
          }}
          maxWidth="lg"
        />
      )}
    </StyledContainer>
  );
};
