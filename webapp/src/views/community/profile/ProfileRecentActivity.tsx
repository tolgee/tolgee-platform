import { useMemo } from 'react';
import { Box, Typography } from '@mui/material';
import { useTranslate } from '@tolgee/react';

import { EmptyListMessage } from 'tg.component/common/EmptyListMessage';
import { useApiInfiniteQuery } from 'tg.service/http/useQueryApi';

import { CommunityActivityItem } from 'tg.views/community/profile/CommunityActivityItem';
import { ProfileShowMore } from 'tg.views/community/profile/ProfileShowMore';

const ACTIVITY_PAGE_SIZE = 20;

type Props = {
  userId: number;
};

export const ProfileRecentActivity = ({ userId }: Props) => {
  const { t } = useTranslate();

  const path = { userId };
  const query = { size: ACTIVITY_PAGE_SIZE };
  const activity = useApiInfiniteQuery({
    url: '/v2/users/{userId}/community-activity',
    method: 'get',
    path,
    query,
    options: {
      getNextPageParam: (lastPage) =>
        lastPage.page && lastPage.page.number! < lastPage.page.totalPages! - 1
          ? { path, query: { ...query, page: lastPage.page.number! + 1 } }
          : null,
    },
  });

  const activities = useMemo(
    () =>
      activity.data?.pages.flatMap((p) => p._embedded?.activities ?? []) ?? [],
    [activity.data]
  );

  return (
    <Box display="grid" gap={1.5} data-cy="community-profile-recent">
      <Typography variant="h4" component="h2" mt={1}>
        {t('community_profile_recent_activity', 'Recent activity')}
      </Typography>
      {activity.isLoading ? (
        <EmptyListMessage loading={true} />
      ) : activities.length === 0 ? (
        <Typography variant="body2" color="text.secondary" fontStyle="italic">
          {t('community_profile_no_activity', 'No activity yet')}
        </Typography>
      ) : (
        <Box
          border={1}
          borderColor="divider1"
          borderRadius="10px"
          py={1}
          display="grid"
          gridTemplateColumns="auto minmax(0, 1fr) auto"
          rowGap="3px"
        >
          {activities.map((a) => (
            <CommunityActivityItem key={a.revisionId} activity={a} />
          ))}
          {activity.hasNextPage && (
            <Box gridColumn="1 / -1" display="grid">
              <ProfileShowMore
                type="button"
                onClick={() => activity.fetchNextPage()}
                disabled={activity.isFetchingNextPage}
                data-cy="community-profile-show-more"
              >
                {t('community_profile_show_more', 'Show more')}
              </ProfileShowMore>
            </Box>
          )}
        </Box>
      )}
    </Box>
  );
};
