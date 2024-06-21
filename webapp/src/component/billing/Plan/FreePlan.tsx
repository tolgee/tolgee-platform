import clsx from 'clsx';
import { Box, styled, useTheme } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { PlanFeature } from 'tg.component/billing/PlanFeature';
import {
  IncludedCreadits,
  IncludedSeats,
  IncludedStrings,
} from 'tg.component/billing/IncludedItem';

import { PlanContainer, PlanContent } from './PlanStyles';
import { PlanActiveBanner } from './PlanActiveBanner';
import { PlanTitle } from './PlanTitle';
import { PlanType } from './types';
import { PricePrimary } from '../Price/PricePrimary';

const StyledPlanContent = styled(PlanContent)`
  display: grid;
  grid-template-areas:
    'title   features   price'
    'strings mt-credits seats';
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
  .strings {
    grid-area: strings;
  }
  .mt-credits {
    grid-area: mt-credits;
  }
  .seats {
    grid-area: seats;
  }

  .features,
  .mt-credits {
    justify-self: center;
  }
  .price,
  .seats {
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

        {plan.includedUsage && (
          <>
            <IncludedStrings
              className="strings"
              count={plan.includedUsage.translations}
              highlightColor={highlightColor}
            />

            <IncludedCreadits
              className="mt-credits"
              count={plan.includedUsage.mtCredits}
              highlightColor={highlightColor}
            />

            <IncludedSeats
              className="seats"
              count={plan.includedUsage.seats}
              highlightColor={highlightColor}
            />
          </>
        )}
      </StyledPlanContent>
    </PlanContainer>
  );
};
