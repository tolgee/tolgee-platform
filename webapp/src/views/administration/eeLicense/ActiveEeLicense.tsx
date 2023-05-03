import { components } from 'tg.service/apiSchema.generated';
import {
  Plan,
  PlanContent,
} from '../../organizations/billing/Subscriptions/common/Plan';
import { ActivePlanTitle } from 'tg.views/organizations/billing/Subscriptions/selfHostedEe/ActivePlanTitle';
import { RefreshButton } from './RefreshButton';
import { ReleaseKeyButton } from './ReleaseKeyButton';
import { PlanInfoArea } from '../../organizations/billing/Subscriptions/common/PlanInfo';
import { IncludedFeatures } from '../../organizations/billing/Subscriptions/selfHostedEe/IncludedFeatures';
import { useTranslate } from '@tolgee/react';
import { Box } from '@mui/material';
import { PlanLicenseKey } from 'tg.views/organizations/billing/Subscriptions/selfHostedEe/PlanLicenseKey';

type EeSubscriptionModel = components['schemas']['EeSubscriptionModel'];

type Props = {
  info: EeSubscriptionModel;
};

export const ActiveEeLicense = ({ info }: Props) => {
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
          <PlanLicenseKey licenseKey={info.licenseKey} defaultOpen={false} />
          <RefreshButton />
          <ReleaseKeyButton />
        </Box>
      </PlanContent>
    </Plan>
  );
};
