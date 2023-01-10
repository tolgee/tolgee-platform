import React, { useMemo } from 'react';
import { styled, Typography, FormControlLabel, Switch } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { UseInfiniteQueryResult } from 'react-query';

import { ActivityCompact } from 'tg.component/activity/ActivityCompact/ActivityCompact';
import { components } from 'tg.service/apiSchema.generated';
import { ActivityDateSeparator } from 'tg.views/projects/dashboard/ActivityDateSeparator';
import { useState } from 'react';
import { useDateCounter } from 'tg.hooks/useDateCounter';
import LoadingButton from 'tg.component/common/form/LoadingButton';

type ProjectActivityModel = components['schemas']['ProjectActivityModel'];
type PagedModelProjectActivityModel =
  components['schemas']['PagedModelProjectActivityModel'];

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

type Props = {
  activityLoadable: UseInfiniteQueryResult<PagedModelProjectActivityModel>;
};

export const ActivityList: React.FC<Props> = ({ activityLoadable }) => {
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
          {activityLoadable.hasNextPage && (
            <StyledLoadingButton
              onClick={() => activityLoadable.fetchNextPage()}
              loading={activityLoadable.isFetchingNextPage}
            >
              <T keyName="global_load_more" />
            </StyledLoadingButton>
          )}
        </StyledList>
      </StyledScroller>
    </StyledContainer>
  );
};
