import { useTranslate } from '@tolgee/react';
import { Box } from '@mui/material';

import { components } from 'tg.service/apiSchema.generated';
import {
  PlanContainer,
  PlanContent,
} from 'tg.views/organizations/billing/Subscriptions/Plan/PlanStyles';
import { PlanInfoArea } from 'tg.views/organizations/billing/Subscriptions/Plan/PlanInfo';
import { IncludedFeatures } from 'tg.views/organizations/billing/Subscriptions/Plan/IncludedFeatures';
import { ActivePlanTitle } from 'tg.views/organizations/billing/Subscriptions/selfHosted/ActiveSubscription/ActivePlanTitle';
import { PlanLicenseKey } from 'tg.views/organizations/billing/Subscriptions/selfHosted/ActiveSubscription/PlanLicenseKey';

import { RefreshButton } from './RefreshButton';
import { ReleaseKeyButton } from './ReleaseKeyButton';

type EeSubscriptionModel = components['schemas']['EeSubscriptionModel'];

type Props = {
  info: EeSubscriptionModel;
};

export const ActiveEeLicense = ({ info }: Props) => {
  const { t } = useTranslate();

  return (
    <PlanContainer data-cy="self-hosted-ee-active-plan">
      <PlanContent>
        <ActivePlanTitle name={info.name} status={info.status} />
        <RefreshButton />
        <PlanInfoArea>
          {info.status === 'ACTIVE' ? (
            <IncludedFeatures features={info.enabledFeatures} />
          ) : (
            t('billing_subscriptions_ee_license_inactive')
          )}
        </PlanInfoArea>
        <Box gridArea="price">
          <ReleaseKeyButton />
        </Box>
        <Box gridArea="action" display="flex" justifyContent="flex-end">
          <PlanLicenseKey licenseKey={info.licenseKey} defaultOpen={false} />
        </Box>
      </PlanContent>
    </PlanContainer>
  );
};
