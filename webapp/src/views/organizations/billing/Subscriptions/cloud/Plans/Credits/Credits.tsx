import { FC, useMemo, useState } from 'react';
import { Box, Link, Slider, styled } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';

import {
  useBillingApiMutation,
  useBillingApiQuery,
} from 'tg.service/http/useQueryApi';
import { useOrganization } from '../../../../../useOrganization';
import { useMoneyFormatter, useNumberFormatter } from 'tg.hooks/useLocale';
import { getPossibleValues } from './creditsUtil';
import { Plan, PlanContent } from '../../../common/Plan';
import { PlanTitle } from '../../../common/PlanTitle';
import { MtHint } from 'tg.component/billing/MtHint';
import { PlanActionButton } from '../PlanActionButton';

const StyledSliderWrapper = styled('div')`
  display: grid;
`;

const StyledPrice = styled('div')`
  display: grid;
  grid-area: price;
`;

const StyledCreditAmount = styled(Box)`
  color: ${({ theme }) => theme.palette.text.secondary};
  font-size: 20px;
`;

const StyledAmount = styled('div')`
  font-size: 18px;
  color: ${({ theme }) => theme.palette.primary.main};
`;

const StyledFullAmount = styled('div')`
  font-size: 13px;
  text-decoration: line-through;
  height: 20px;
`;

export const Credits: FC = () => {
  const organization = useOrganization();

  const pricesLoadable = useBillingApiQuery({
    url: '/v2/public/billing/mt-credit-prices',
    method: 'get',
  });

  const buyMutation = useBillingApiMutation({
    url: `/v2/organizations/{organizationId}/billing/buy-more-credits`,
    method: 'post',
    options: {
      onSuccess(data) {
        window.location.href = data.url;
      },
    },
  });

  const { t } = useTranslate();

  const buy = (priceId: number, amount: number) => {
    buyMutation.mutate({
      path: {
        organizationId: organization!.id,
      },
      content: {
        'application/json': {
          priceId: priceId,
          amount: amount,
        },
      },
    });
  };

  const [sliderValue, setSliderValue] = useState(1 as number);

  const sliderPossibleValues = useMemo(() => {
    const prices = pricesLoadable?.data?._embedded?.prices;
    if (!prices) {
      return null;
    }
    return getPossibleValues(prices);
  }, [pricesLoadable?.data?._embedded?.prices]);

  const formatValue = (value) =>
    t({
      key: 'billing_buy_more_mt_slider_value',
      defaultValue: '{amount} Credits',
      params: { amount: value },
    });

  const formatPrice = useMoneyFormatter();
  const formatNumber = useNumberFormatter();

  if (!sliderPossibleValues) {
    return null;
  }

  const { totalPrice, regularPrice, totalAmount, priceId, itemQuantity } =
    sliderPossibleValues?.[sliderValue];

  return (
    <Plan>
      <PlanContent>
        <Box gridArea="title" gridColumn="1 / span 2">
          <PlanTitle
            title={
              <T
                keyName="billing_extra_credits_title"
                params={{ hint: <MtHint /> }}
              />
            }
          />
        </Box>
        <Box gridArea="info">
          <Box mb={2}>
            <T
              keyName="billing_credits_explanation_tolgee_unified"
              params={{
                link: (
                  <Link
                    href="https://tolgee.io/pricing#pricing-question-what-is-tolgee-translator"
                    target="_blank"
                    rel="noopener noreferrer"
                  />
                ),
              }}
            />
          </Box>
          <StyledSliderWrapper>
            <StyledCreditAmount>{formatNumber(totalAmount)}</StyledCreditAmount>
            <Slider
              value={sliderValue}
              min={0}
              step={1}
              max={sliderPossibleValues.length - 1}
              scale={(value) => sliderPossibleValues[value].totalAmount}
              getAriaValueText={formatValue}
              onChange={(_, value) => setSliderValue(value as number)}
              aria-labelledby="non-linear-slider"
            />
          </StyledSliderWrapper>
        </Box>
        <StyledPrice>
          <StyledAmount>{formatPrice(totalPrice)}</StyledAmount>
          <StyledFullAmount>
            {regularPrice !== undefined && formatPrice(regularPrice)}
          </StyledFullAmount>
        </StyledPrice>
        <PlanActionButton
          variant="contained"
          color="primary"
          size="small"
          onClick={() => buy(priceId, itemQuantity)}
          loading={buyMutation.isLoading}
          data-cy="billing-extra-credits-buy"
        >
          {t('billing_extra_credits_buy')}
        </PlanActionButton>
      </PlanContent>
    </Plan>
  );
};
