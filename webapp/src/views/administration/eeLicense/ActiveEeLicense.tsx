import { FC } from 'react';
import { components } from 'tg.service/apiSchema.generated';
import {
  Plan,
  PlanContent,
} from '../../organizations/billing/Subscriptions/common/Plan';
import { StyledActionArea } from '../../organizations/billing/Subscriptions/cloud/Plans/PlanActionButton';
import { ActivePlanTitle } from 'tg.views/organizations/billing/Subscriptions/selfHostedEe/ActivePlanTitle';
import { RefreshButton } from './RefreshButton';
import { ReleaseKeyButton } from './ReleaseKeyButton';
import { PlanInfoArea } from '../../organizations/billing/Subscriptions/common/PlanInfo';
import { IncludedFeatures } from '../../organizations/billing/Subscriptions/selfHostedEe/IncludedFeatures';
import { useTranslate } from '@tolgee/react';

export const ActiveEeLicense: FC<{
  info: components['schemas']['EeSubscriptionModel'];
}> = ({ info }) => {
  const { t } = useTranslate();

  return (
    <Plan>
      <PlanContent>
        <ActivePlanTitle
          name={info.name}
          status={info.status}
          licenseKey={info.licenseKey}
        />
        <PlanInfoArea>
          {info.status === 'ACTIVE' ? (
            <IncludedFeatures
              features={info.enabledFeatures}
            ></IncludedFeatures>
          ) : (
            t('billing_subscriptions_ee_license_inactive')
          )}
        </PlanInfoArea>
        <StyledActionArea>
          <RefreshButton />
          <ReleaseKeyButton />
        </StyledActionArea>
      </PlanContent>
    </Plan>
  );
};
