import { FC } from 'react';
import { components } from 'tg.service/apiSchema.generated';
import { Plan, PlanContent } from 'tg.component/billing/plan/Plan';
import { ActivePlanTitle } from 'tg.views/organizations/billing/Subscriptions/selfHostedEe/ActivePlanTitle';
import { RefreshButton } from './RefreshButton';
import { ReleaseKeyButton } from './ReleaseKeyButton';
import { PlanInfoArea } from 'tg.component/billing/plan/PlanInfo';
import { IncludedFeatures } from '../../../component/billing/plan/IncludedFeatures';
import { useTranslate } from '@tolgee/react';
import { Box } from '@mui/material';
import { PlanLicenseKey } from 'tg.views/organizations/billing/Subscriptions/selfHostedEe/PlanLicenseKey';

export const ActiveEeLicense: FC<{
  info: components['schemas']['EeSubscriptionModel'];
}> = ({ info }) => {
  const { t } = useTranslate();

  return (
    <Plan>
      <PlanContent>
        <ActivePlanTitle name={info.name} status={info.status} />
        <PlanInfoArea>
          {info.status === 'ACTIVE' ? (
            <IncludedFeatures features={info.enabledFeatures} />
          ) : (
            t('billing_subscriptions_ee_license_inactive')
          )}
        </PlanInfoArea>
        <Box
          display="flex"
          gap={1}
          gridArea="price / price / span 1 / span 2"
          justifyContent="end"
        >
          <PlanLicenseKey licenseKey={info.licenseKey} />
          <RefreshButton />
          <ReleaseKeyButton />
        </Box>
      </PlanContent>
    </Plan>
  );
};
