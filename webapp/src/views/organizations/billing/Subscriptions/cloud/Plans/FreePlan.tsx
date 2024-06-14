import clsx from 'clsx';
import { Box, styled } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { PlanFeature } from 'tg.component/billing/PlanFeature';
import {
  IncludedCreadits,
  IncludedSeats,
  IncludedStrings,
} from 'tg.component/billing/IncludedItem';

import { Plan, PlanContent } from '../../common/Plan';
import { PlanTitle } from '../../common/PlanTitle';
import { PlanActiveBanner } from '../../common/PlanActiveBanner';
import { PlanType } from './types';
import { PricePrimary } from './Price/PricePrimary';

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
`;

type Props = {
  plan: PlanType;
  isActive: boolean;
  isEnded: boolean;
};

export const FreePlan = ({ plan, isActive, isEnded }: Props) => {
  const { t } = useTranslate();
  return (
    <Plan className={clsx({ active: isActive })} data-cy="billing-plan">
      <PlanActiveBanner isActive={isActive} isEnded={isEnded} />

      <StyledPlanContent>
        <Box className="title">
          <PlanTitle title={plan.name} />
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
            <PricePrimary prices={plan.prices} period="MONTHLY" />
          </Box>
        )}

        {plan.includedUsage && (
          <>
            <IncludedStrings
              className="strings"
              count={plan.includedUsage.translations}
            />

            <IncludedCreadits
              className="mt-credits"
              count={plan.includedUsage.mtCredits}
            />

            <IncludedSeats className="seats" count={plan.includedUsage.seats} />
          </>
        )}
      </StyledPlanContent>
    </Plan>
  );
};
