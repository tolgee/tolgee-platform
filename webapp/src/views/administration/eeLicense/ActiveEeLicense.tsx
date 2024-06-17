import { components } from 'tg.service/apiSchema.generated';
import {
  Plan,
  PlanContent,
} from '../../organizations/billing/Subscriptions/common/Plan';
import { ActivePlanTitle } from 'tg.views/organizations/billing/Subscriptions/selfHostedEe/ActivePlanTitle';
import { RefreshButton } from './RefreshButton';
import { ReleaseKeyButton } from './ReleaseKeyButton';
import { PlanInfoArea } from '../../organizations/billing/Subscriptions/common/PlanInfo';
import { IncludedFeatures } from '../../organizations/billing/Subscriptions/common/IncludedFeatures';
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
    <Plan data-cy="self-hosted-ee-active-plan">
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
    </Plan>
  );
};
