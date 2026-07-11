import { usePreferredOrganization } from 'tg.globalContext/helpers';
import { LINKS, PARAMS } from 'tg.constants/links';
import { useRouteMatch } from 'react-router-dom';
import { useTestClock } from 'tg.service/useTestClock';
import { Theme, useMediaQuery } from '@mui/material';

export const useTrialInfo = () => {
  const { preferredOrganization } = usePreferredOrganization();

  const subscriptionsLink = LINKS.ORGANIZATION_SUBSCRIPTIONS.build({
    [PARAMS.ORGANIZATION_SLUG]: preferredOrganization?.slug ?? '',
  });

  const isCurrentSubscriptionPage = useRouteMatch(subscriptionsLink);

  const testClock = useTestClock();

  const isSmallScreen = useMediaQuery((theme: Theme) =>
    theme.breakpoints.down('md')
  );

  const activeCloudSubscription =
    preferredOrganization?.activeCloudSubscription;

  const trialEnd = activeCloudSubscription?.trialEnd;

  const shouldShowChip =
    trialEnd && activeCloudSubscription?.status == 'TRIALING';

  function getDaysLeft() {
    if (!trialEnd) {
      return 0;
    }
    const currentTime = testClock || new Date().getTime();

    const msToTrialEnd = trialEnd - currentTime;

    return Math.floor(msToTrialEnd / (1000 * 60 * 60 * 24));
  }

  const daysLeft = getDaysLeft();

  const shouldShowAnnouncement =
    shouldShowChip &&
    !isCurrentSubscriptionPage &&
    !isSmallScreen &&
    daysLeft < 7;

  return {
    daysLeft,
    subscriptionsLink,
    shouldShowAnnouncement,
    shouldShowChip,
    isCurrentSubscriptionPage,
  };
};
