import { styled, Box } from '@mui/material';

import { ActivityUser } from '../ActivityUser';
import { ActivityEntities } from './ActivityEntities';
import {
  Activity,
  ActivityModel,
  Entity,
  EntityEnum,
  EntityOptions,
} from '../types';
import { useApiInfiniteQuery } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';
import { useEffect, useMemo } from 'react';
import { buildEntity } from '../activityTools';
import { activityEntities } from '../activityEntities';
import { BoxLoading } from 'tg.component/common/BoxLoading';
import { useInView } from 'react-intersection-observer';

const StyledContainer = styled('div')`
  display: grid;
  gap: 3px;
  & > * {
    overflow: hidden;
  }

  & .showOnMouseOver {
    opacity: 0;
    pointer-events: none;
    transition: opacity 0.1s ease-out;
  }

  &:hover .showOnMouseOver {
    opacity: 1;
    pointer-events: all;
    transition: opacity 0.3s ease-in;
  }
`;

const StyledUserWrapper = styled(Box)`
  display: flex;
`;

type Props = {
  data: ActivityModel;
  activity: Activity;
  diffEnabled: boolean;
};

export const ActivityDetail = ({ data, diffEnabled, activity }: Props) => {
  const { ref, inView } = useInView({ rootMargin: '100px' });
  const project = useProject();

  const isBatch = !data.modifiedEntities;

  const path = { projectId: project.id, revisionId: data.revisionId };
  const query = { size: 40 };
  const detailLoadable = useApiInfiniteQuery({
    url: '/v2/projects/{projectId}/activity/revisions/{revisionId}/modified-entities',
    method: 'get',
    path,
    query,
    options: {
      enabled: isBatch,
      cacheTime: 0,
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

  useEffect(() => {
    if (inView) {
      detailLoadable.fetchNextPage();
    }
  }, [inView]);

  const activityWithData = useMemo(() => {
    const entities = detailLoadable.data?.pages.flatMap((p) => {
      return p._embedded?.modifiedEntities
        ?.map((e) => {
          const options = activityEntities[e.entityClass] as
            | EntityOptions
            | undefined;
          if (options) {
            return buildEntity(
              e.entityClass as EntityEnum,
              e,
              options,
              Object.keys(options?.fields || {})
            );
          }
        })
        .filter(Boolean) as Entity[];
    });
    return { ...activity, entities: entities ?? activity.entities };
  }, [detailLoadable.data]);

  return (
    <StyledContainer data-cy="activity-detail">
      <StyledUserWrapper>
        <ActivityUser item={data} />
      </StyledUserWrapper>
      {detailLoadable.isLoading ? (
        <BoxLoading />
      ) : (
        <>
          <ActivityEntities
            activity={activityWithData}
            diffEnabled={diffEnabled}
            showAllReferences={isBatch}
          />
          {detailLoadable.hasNextPage && (
            <Box display="flex" justifyContent="center" mt={3} ref={ref}>
              <BoxLoading />
            </Box>
          )}
        </>
      )}
    </StyledContainer>
  );
};
