import { usePreferredOrganization } from 'tg.globalContext/helpers';
import { billingLinkFor } from 'tg.fixtures/billingLink';
import { useRouteMatch } from 'react-router-dom';
import { memberCloudSubscription } from 'tg.fixtures/organizationEntitlement';
import { useTestClock } from 'tg.service/useTestClock';
import { Theme, useMediaQuery } from '@mui/material';

export const useTrialInfo = () => {
  const { preferredOrganization } = usePreferredOrganization();

  const subscriptionsLink = billingLinkFor({
    slug: preferredOrganization?.slug ?? '',
  });

  const isCurrentSubscriptionPage = useRouteMatch(subscriptionsLink);

  const testClock = useTestClock();

  const isSmallScreen = useMediaQuery((theme: Theme) =>
    theme.breakpoints.down('md')
  );

  const activeCloudSubscription = memberCloudSubscription(
    preferredOrganization
  );

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
