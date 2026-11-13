import { useEffect, useState } from 'react';
import { Alert, Box, Button, useMediaQuery, useTheme } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { Link, useRouteMatch } from 'react-router-dom';

import { LINKS, PARAMS } from 'tg.constants/links';
import { DashboardPage } from 'tg.component/layout/DashboardPage';
import { BaseView } from 'tg.component/layout/BaseView';
import { EmptyListMessage } from 'tg.component/common/EmptyListMessage';
import { useGlobalActions } from 'tg.globalContext/GlobalContext';
import { useHasPublicContribution, useUser } from 'tg.globalContext/helpers';
import { useApiQuery } from 'tg.service/http/useQueryApi';

import { ProfileIdentity } from 'tg.views/community/profile/ProfileIdentity';
import { ProfileTotals } from 'tg.views/community/profile/ProfileTotals';
import { ProfileActivityCalendar } from 'tg.views/community/profile/ProfileActivityCalendar';
import { ProfileRecentActivity } from 'tg.views/community/profile/ProfileRecentActivity';

export const CommunityProfileView = () => {
  const match = useRouteMatch();
  const userId = Number(match.params[PARAMS.USER_ID]);
  const user = useUser();
  const isMe = user?.id === userId;
  const viewerHasPublicContribution = useHasPublicContribution();
  const theme = useTheme();
  const compact = useMediaQuery(theme.breakpoints.down('md'));
  const { t } = useTranslate();

  const currentYear = new Date().getFullYear();
  const [year, setYear] = useState(currentYear);

  const profile = useApiQuery({
    url: '/v2/users/{userId}/community-profile',
    method: 'get',
    path: { userId },
    query: {
      from: `${year}-01-01`,
      to: year === currentYear ? undefined : `${year}-12-31`,
      granularity: 'DAY',
    },
    fetchOptions: { disable404Redirect: true, disableErrorNotification: true },
    options: { retry: false, keepPreviousData: true, staleTime: 5 * 60_000 },
  });

  const data = profile.data;
  const { refetchInitialData } = useGlobalActions();
  useEffect(() => {
    if (isMe && data && !viewerHasPublicContribution) {
      refetchInitialData();
    }
  }, [isMe, !!data, viewerHasPublicContribution]);
  const profileVisibility = user?.profileVisibility;
  const isOwnProfilePublic =
    isMe &&
    (profileVisibility === 'PUBLIC' ||
      (profileVisibility === 'DEFAULT' && viewerHasPublicContribution));

  const content = () => {
    if (profile.isLoading) {
      return <EmptyListMessage loading={true} />;
    }
    if (!data || (isMe && data.projects.length === 0)) {
      return isMe ? (
        <Box data-cy="community-profile-empty">
          <EmptyListMessage
            hint={t(
              'community_profile_empty_hint',
              'Find a community project and help translate it.'
            )}
          >
            {t(
              'community_profile_empty',
              'You haven’t contributed to a community project yet'
            )}
          </EmptyListMessage>
        </Box>
      ) : (
        <Box data-cy="community-profile-unavailable">
          <EmptyListMessage>
            {t(
              'community_profile_unavailable',
              'This profile is not available'
            )}
          </EmptyListMessage>
        </Box>
      );
    }

    return (
      <Box
        display="grid"
        gridTemplateColumns={
          compact ? 'minmax(0, 1fr)' : '300px minmax(0, 1fr)'
        }
        gap={5}
        alignItems="start"
      >
        <ProfileIdentity
          profile={data}
          isMe={isMe}
          isPublic={isOwnProfilePublic}
        />
        <Box display="grid" gap={3} minWidth={0}>
          {isMe && !isOwnProfilePublic && (
            <Alert
              severity="info"
              data-cy="community-profile-private"
              action={
                <Button
                  component={Link}
                  to={LINKS.USER_PROFILE.build()}
                  color="inherit"
                  size="small"
                >
                  {t('community_profile_private_action', 'Change')}
                </Button>
              }
            >
              {t(
                'community_profile_private_banner',
                'Your profile is private. Only you can see it.'
              )}
            </Alert>
          )}
          <ProfileTotals profile={data} />
          <ProfileActivityCalendar
            buckets={data.buckets}
            year={year}
            onYearChange={setYear}
          />
          <ProfileRecentActivity userId={userId} />
        </Box>
      </Box>
    );
  };

  return (
    <DashboardPage>
      <BaseView windowTitle={data?.name ?? ''} maxWidth="wide">
        <Box pt={{ xs: 1, sm: 3 }} pb={6} data-cy="community-profile-page">
          {content()}
        </Box>
      </BaseView>
    </DashboardPage>
  );
};

export default CommunityProfileView;
