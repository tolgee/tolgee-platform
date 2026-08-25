import {
  useBillingApiMutation,
  useBillingApiQuery,
} from 'tg.service/http/useQueryApi';
import { usePreferredOrganization } from 'tg.globalContext/helpers';
import { components } from 'tg.service/billingApiSchema.generated';

type Props = {
  enabled: boolean;
  wordsExhausted: boolean;
};

type Subscription = components['schemas']['CloudSubscriptionModel'] | undefined;

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

  const subscription = subscriptionLoadable.data;

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
    enable,
    isEnabling: autoUpgradeMutation.isLoading,
  };
};
