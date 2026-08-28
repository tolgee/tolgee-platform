import { useTranslate } from '@tolgee/react';
import { LINKS } from 'tg.constants/links';
import { useGlobalContext } from 'tg.globalContext/GlobalContext';
import {
  useCanSeeBilling,
  useIsAdmin,
  useIsBillingEnabled,
} from 'tg.globalContext/helpers';

export function useFeatureMissingExplanation() {
  const subscription = useGlobalContext((c) => c.initialData.eeSubscription);
  const isAdmin = useIsAdmin();
  const billingEnabled = useIsBillingEnabled();
  const canSeeBilling = useCanSeeBilling();

  const { t } = useTranslate();

  const ifAdmin = <T,>(value: T) => (isAdmin ? value : undefined);

  const ifCanSeeBilling = <T,>(value: T) => (canSeeBilling ? value : undefined);

  if (billingEnabled) {
    return {
      message: t('feature-explanation-plan-not-sufficient'),
      actionTitle: ifCanSeeBilling(
        t('feature-explanation-upgrade-subscription')
      ),
      link: ifCanSeeBilling(LINKS.GO_TO_CLOUD_BILLING.build()),
    };
  }

  if (subscription) {
    return {
      message:
        subscription.status === 'ACTIVE'
          ? t('feature-explanation-license-not-sufficient')
          : t('feature-explanation-license-not-active'),
      actionTitle: ifAdmin(t('feature-explanation-check-license-action')),
      link: ifAdmin(LINKS.ADMINISTRATION_EE_LICENSE.build()),
    };
  }

  if (isAdmin) {
    return {
      message: t('feature-explanation-no-license'),
      actionTitle: t('feature-explanation-setup-license'),
      link: LINKS.ADMINISTRATION_EE_LICENSE.build(),
    };
  }

  return {};
}
