import { useTranslate } from '@tolgee/react';
import { PlanSubtitle } from './PlanStyles';

type Props = {
  isActive: boolean;
  isEnded: boolean;
};

export const PlanActiveBanner = ({ isActive, isEnded }: Props) => {
  const { t } = useTranslate();
  return (
    <>
      {isActive && (
        <PlanSubtitle data-cy="billing-plan-subtitle">
          {isEnded
            ? t('billing_subscription_cancelled')
            : t('billing_subscription_active')}
        </PlanSubtitle>
      )}
    </>
  );
};
