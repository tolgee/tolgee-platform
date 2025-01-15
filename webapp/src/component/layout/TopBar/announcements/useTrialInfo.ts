import { usePreferredOrganization } from 'tg.globalContext/helpers';
import { LINKS, PARAMS } from 'tg.constants/links';
import { useRouteMatch } from 'react-router-dom';
import { useTestClock } from 'tg.service/useTestClock';

export const useTrialInfo = () => {
  const { preferredOrganization } = usePreferredOrganization();

  const subscriptionsLink = LINKS.ORGANIZATION_SUBSCRIPTIONS.build({
    [PARAMS.ORGANIZATION_SLUG]: preferredOrganization?.slug ?? '',
  });

  const isCurrentSubscriptionPage = useRouteMatch(subscriptionsLink);

  const testClock = useTestClock();

  const activeCloudSubscription =
    preferredOrganization?.activeCloudSubscription;
  const trialEnd = activeCloudSubscription?.trialEnd;
  const hasPaymentMethod = activeCloudSubscription?.hasPaymentMethod;

  const shouldShowChip =
    trialEnd && activeCloudSubscription?.status == 'TRIALING';
  const shouldShowAnnouncement =
    shouldShowChip && !isCurrentSubscriptionPage && !hasPaymentMethod;

  function getDaysLeft() {
    if (!trialEnd) {
      return 0;
    }
    const currentTime = testClock || new Date().getTime();

    const msToTrialEnd = trialEnd - currentTime;

    return Math.floor(msToTrialEnd / (1000 * 60 * 60 * 24));
  }

  return {
    daysLeft: getDaysLeft(),
    subscriptionsLink,
    shouldShowAnnouncement,
    shouldShowChip,
    isCurrentSubscriptionPage,
  };
};
