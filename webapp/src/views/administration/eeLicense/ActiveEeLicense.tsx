import { useTranslate } from '@tolgee/react';
import { Box, styled, useTheme } from '@mui/material';

import { components } from 'tg.service/apiSchema.generated';
import {
  PlanContainer,
  PlanContent,
  PlanFeaturesBox,
} from 'tg.component/billing/Plan/PlanStyles';
import { ActivePlanTitle } from 'tg.component/billing/ActiveSubscription/ActivePlanTitle';
import { PlanLicenseKey } from 'tg.component/billing/ActiveSubscription/PlanLicenseKey';
import { ActiveSubscriptionBanner } from 'tg.component/billing/ActiveSubscription/ActiveSubscriptionBanner';
import { IncludedFeatures } from 'tg.component/billing/Plan/IncludedFeatures';

import { RefreshButton } from './RefreshButton';
import { ReleaseKeyButton } from './ReleaseKeyButton';
import clsx from 'clsx';

type EeSubscriptionModel = components['schemas']['EeSubscriptionModel'];

const StyledFeatures = styled(IncludedFeatures)`
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(200px, 100%), 1fr));
  margin: 0px;
`;

type Props = {
  info: EeSubscriptionModel;
};

export const ActiveEeLicense = ({ info }: Props) => {
  const { t } = useTranslate();
  const theme = useTheme();

  const highlightColor = theme.palette.primary.main;

  return (
    <PlanContainer
      data-cy="self-hosted-ee-active-plan"
      className={clsx({ active: info.status === 'ACTIVE' })}
    >
      <ActiveSubscriptionBanner status={info.status} />
      <PlanContent sx={{ gridTemplateColumns: '1fr' }}>
        <Box display="flex" justifyContent="space-between">
          <ActivePlanTitle
            name={info.name}
            status={info.status}
            highlightColor={highlightColor}
          />
          <RefreshButton />
        </Box>
        {info.status === 'ACTIVE' ? (
          <PlanFeaturesBox sx={{ gap: '18px', mb: 1 }}>
            <StyledFeatures features={info.enabledFeatures} />
          </PlanFeaturesBox>
        ) : (
          <Box>{t('billing_subscriptions_ee_license_inactive')}</Box>
        )}
        <Box display="flex" justifyContent="flex-end" gap={1} mt={2}>
          <ReleaseKeyButton />
          <PlanLicenseKey licenseKey={info.licenseKey} defaultOpen={false} />
        </Box>
      </PlanContent>
    </PlanContainer>
  );
};
