import { FC } from 'react';
import clsx from 'clsx';

import { BillingPeriodType } from '../Price/PeriodSwitch';
import {
  PlanContainer,
  PlanContent,
  PlanFeaturesBox,
  PlanTitle,
} from './PlanStyles';
import { PlanPrice } from '../Price/PlanPrice';
import { IncludedFeatures } from './IncludedFeatures';
import { PlanActiveBanner } from './PlanActiveBanner';
import { ShowAllFeaturesLink } from './ShowAllFeatures';
import { PlanType } from './types';
import { IncludedUsage } from './IncludedUsage';
import { ContactUsButton } from './ContactUsButton';
import { Box, Chip, Theme, Tooltip, useTheme } from '@mui/material';
import { useTranslate } from '@tolgee/react';

type Features = PlanType['enabledFeatures'];

type Props = {
  plan: PlanType;
  period: BillingPeriodType;
  onPeriodChange: (period: BillingPeriodType) => void;
  active: boolean;
  ended: boolean;
  filteredFeatures: Features;
  topFeature?: React.ReactNode;
  featuresMinHeight?: string;
  action: React.ReactNode;
  custom?: boolean;
  nonCommercial: boolean;
  activeTrial?: boolean;
};

export const getHighlightColor = (theme: Theme, custom: boolean) =>
  custom ? theme.palette.tokens.info.main : theme.palette.tokens.primary.main;

export const Plan: FC<Props> = ({
  plan,
  period,
  onPeriodChange,
  active,
  ended,
  filteredFeatures,
  topFeature,
  featuresMinHeight,
  action,
  custom,
  nonCommercial,
  activeTrial,
}) => {
  const { t } = useTranslate();
  const theme = useTheme();

  const highlightColor = getHighlightColor(theme, !!custom);

  return (
    <PlanContainer className={clsx({ active })} data-cy="billing-plan">
      <PlanActiveBanner
        active={active}
        ended={ended}
        custom={custom}
        activeTrial={activeTrial}
      />
      <PlanContent>
        <PlanTitle sx={{ pb: '10px', color: highlightColor }}>
          {plan.name}
        </PlanTitle>

        {nonCommercial && (
          <Box display="flex" justifyContent="center">
            <Tooltip title={t('billing_plan_non_commercial_hint')}>
              <Chip
                label={t('billing_plan_non_commercial_label')}
                size="small"
                color="success"
              />
            </Tooltip>
          </Box>
        )}

        <Box
          display="flex"
          flexDirection="column"
          alignItems="stretch"
          flexGrow={1}
          sx={{ pt: '10px' }}
        >
          <PlanFeaturesBox sx={{ gap: '18px' }}>
            <IncludedFeatures
              sx={{ minHeight: featuresMinHeight }}
              features={filteredFeatures}
              topFeature={topFeature}
            />
            {plan.public && (
              <ShowAllFeaturesLink sx={{ alignSelf: 'center' }} />
            )}
            <IncludedUsage
              metricType={plan.metricType}
              includedUsage={plan.includedUsage}
              highlightColor={highlightColor}
              sx={{ alignSelf: 'center' }}
            />
          </PlanFeaturesBox>
        </Box>

        {plan.prices && (
          <PlanPrice
            sx={{ paddingTop: '20px' }}
            prices={plan.prices}
            period={period}
            onPeriodChange={onPeriodChange}
            highlightColor={highlightColor}
          />
        )}

        <Box sx={{ paddingTop: '20px', alignSelf: 'center' }}>
          {plan.type === 'CONTACT_US' ? <ContactUsButton /> : action}
        </Box>
      </PlanContent>
    </PlanContainer>
  );
};
