import clsx from 'clsx';
import { Box, styled, useTheme } from '@mui/material';
import { useTranslate } from '@tolgee/react';

import { PlanContainer, PlanContent } from '../PlanStyles';
import { PlanActiveBanner } from '../PlanActiveBanner';
import { PlanTitle } from '../PlanTitle';
import { PlanType } from '../types';
import { PricePrimary } from '../../Price/PricePrimary';
import { PlanFeature } from '../../PlanFeature';
import { FreePlanLimits } from './FreePlanLimits';

const StyledPlanContent = styled(PlanContent)`
  display: grid;
  grid-template-areas:
    'title   features   price'
    'metric1 metric2  metric3';
  grid-template-rows: unset;
  align-items: center;

  .title {
    grid-area: title;
  }

  .features {
    grid-area: features;
  }

  .price {
    grid-area: price;
  }

  .features {
    justify-self: center;
  }

  .price {
    justify-self: end;
  }

  @container (max-width: 699px) {
    gap: 8px;
    grid-template-areas:
      'title'
      'features'
      'strings'
      'mt-credits'
      'seats'
      'price';

    .title,
    .strings,
    .mt-credits,
    .seats,
    .features {
      justify-self: start;
    }

    .price {
      justify-self: end;
    }
  }
`;

type Props = {
  plan: PlanType;
  active: boolean;
  ended: boolean;
};

export const FreePlan = ({ plan, active, ended }: Props) => {
  const { t } = useTranslate();
  const theme = useTheme();
  const highlightColor = theme.palette.primary.main;
  return (
    <PlanContainer className={clsx({ active })} data-cy="billing-plan">
      <PlanActiveBanner active={active} ended={ended} />

      <StyledPlanContent>
        <Box className="title">
          <PlanTitle sx={{ color: highlightColor }} title={plan.name} />
        </Box>
        <Box className="features" display="flex" gap="4px">
          <PlanFeature
            bold
            link="https://tolgee.io/pricing#features-table"
            name={t('billing_subscriptions_all_essentials')}
          />
        </Box>
        {plan.prices && (
          <Box className="price">
            <PricePrimary
              prices={plan.prices}
              period="MONTHLY"
              highlightColor={highlightColor}
            />
          </Box>
        )}

        <FreePlanLimits plan={plan} />
      </StyledPlanContent>
    </PlanContainer>
  );
};
