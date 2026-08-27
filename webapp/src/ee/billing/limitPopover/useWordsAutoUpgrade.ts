import {
  useBillingApiMutation,
  useBillingApiQuery,
} from 'tg.service/http/useQueryApi';
import { usePreferredOrganization } from 'tg.globalContext/helpers';
import { components } from 'tg.service/billingApiSchema.generated';
import { isLargestTier } from 'tg.billing/component/Plan/largestTier';

type Props = {
  enabled: boolean;
  wordsExhausted: boolean;
};

type Subscription = components['schemas']['CloudSubscriptionModel'] | undefined;

/**
 * The subscription's own plan model is assembled from the active tier alone, so it carries neither
 * `tiers` nor `currentTierId`. The ladder only comes back from the plans listing.
 */
type Offering = {
  tiers?: { id: number; includedWords: number }[];
  currentTierId?: number | null;
};

const isWordPlan = (subscription: Subscription) =>
  Boolean(
    subscription &&
      subscription.plan.metricType === 'HOSTED_WORDS' &&
      !subscription.plan.free
  );

export const isWordsAutoUpgradeAvailable = (
  subscription: Subscription,
  wordsExhausted: boolean
) =>
  Boolean(isWordPlan(subscription) && !subscription!.autoUpgradeEnabled) &&
  wordsExhausted;

/**
 * The server only blocks a word-exhausted write when auto-upgrade cannot cover it, so a customer
 * who is blocked *with* the toggle already on has hit one of the cases it does not apply to —
 * largest tier, cancelling, or a scheduled downgrade. Offering them the toggle would be a no-op.
 */
export const isWordsAutoUpgradeIneffective = (
  subscription: Subscription,
  wordsExhausted: boolean
) =>
  Boolean(isWordPlan(subscription) && subscription!.autoUpgradeEnabled) &&
  wordsExhausted;

export type WordsAutoUpgradeReason =
  | 'largestTier'
  | 'scheduledChange'
  | 'other';

/**
 * Why auto-upgrade cannot be applied, so the limit dialog can say something specific.
 *
 * The largest tier is checked first even when a plan change is also pending: cancelling that change
 * only helps if there is a bigger tier for auto-upgrade to move to, so telling someone already at
 * the top to cancel it would send them to undo something that changes nothing.
 */
export const wordsAutoUpgradeIneffectiveReason = (
  subscription: Subscription,
  offering?: Offering
): WordsAutoUpgradeReason => {
  if (isLargestTier(offering?.tiers, offering?.currentTierId)) {
    return 'largestTier';
  }
  if (subscription?.scheduledDowngrade) {
    return 'scheduledChange';
  }
  return 'other';
};

export const useWordsAutoUpgrade = ({ enabled, wordsExhausted }: Props) => {
  const { preferredOrganization } = usePreferredOrganization();
  const organizationId = preferredOrganization?.id;

  const subscriptionLoadable = useBillingApiQuery({
    url: '/v2/organizations/{organizationId}/billing/subscription',
    method: 'get',
    path: {
      organizationId: organizationId ?? 0,
    },
    options: {
      enabled: enabled && wordsExhausted && organizationId !== undefined,
    },
  });

  const plansLoadable = useBillingApiQuery({
    url: '/v2/organizations/{organizationId}/billing/plans',
    method: 'get',
    path: {
      organizationId: organizationId ?? 0,
    },
    options: {
      enabled: enabled && wordsExhausted && organizationId !== undefined,
    },
  });

  const subscription = subscriptionLoadable.data;
  const offering = plansLoadable.data?._embedded?.plans?.find((candidate) =>
    Boolean(candidate.currentTierId)
  );

  const available = isWordsAutoUpgradeAvailable(subscription, wordsExhausted);
  const ineffective = isWordsAutoUpgradeIneffective(
    subscription,
    wordsExhausted
  );

  const autoUpgradeMutation = useBillingApiMutation({
    url: '/v2/organizations/{organizationId}/billing/auto-upgrade',
    method: 'put',
    invalidatePrefix: '/v2/organizations',
  });

  const enable = (onSuccess: () => void) => {
    if (organizationId === undefined) {
      return;
    }
    autoUpgradeMutation.mutate(
      {
        path: { organizationId },
        content: { 'application/json': { enabled: true } },
      },
      { onSuccess }
    );
  };

  return {
    available,
    ineffective,
    reason: wordsAutoUpgradeIneffectiveReason(subscription, offering),
    enable,
    isEnabling: autoUpgradeMutation.isLoading,
  };
};
