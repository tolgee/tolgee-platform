import { FC, useMemo, useState } from 'react';
import {
  useBillingApiMutation,
  useBillingApiQuery,
} from '../useBillingQueryApi';
import { Box, Slider, Typography } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { useOrganization } from 'tg.views/organizations/useOrganization';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { getPossibleValues } from './creditsUtil';
import { useApiQuery } from 'tg.service/http/useQueryApi';

export const MoreMtCredits: FC = () => {
  const organization = useOrganization();

  const actualCreditBalanceLoadable = useApiQuery({
    url: '/v2/organizations/{organizationId}/machine-translation-credit-balance',
    method: 'get',
    path: {
      organizationId: organization!.id,
    },
  });

  const pricesLoadable = useBillingApiQuery({
    url: '/v2/billing/mt-credit-prices',
    method: 'get',
  });

  const buyMutation = useBillingApiMutation({
    url: `/v2/organizations/{organizationId}/billing/buy-more-credits`,
    method: 'post',
    options: {
      onSuccess(data) {
        window.location.href = data;
      },
    },
  });

  const t = useTranslate();

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
      parameters: { amount: value },
    });

  return (
    <>
      <Typography variant="h3">More credits</Typography>

      {actualCreditBalanceLoadable.data &&
        t({
          key: 'billing_mt_credits_actual_balance',
          defaultValue: 'Actual balance: {amount, number} Credits',
          parameters: {
            amount: actualCreditBalanceLoadable.data.additionalCreditBalance,
          },
        })}

      {sliderPossibleValues && (
        <>
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

          <Box>
            {t({
              key: 'billing_mt_credits_amount',
              defaultValue: '{amount, number} Credits',
              parameters: {
                amount: sliderPossibleValues[sliderValue].totalAmount,
              },
            })}
          </Box>
          <Box>
            {t({
              key: 'billing_mt_credits_price',
              defaultValue: 'Price: {price, number, :: currency/EUR}',
              parameters: {
                price: sliderPossibleValues[sliderValue].totalPrice,
              },
            })}
          </Box>
          <Box>
            {t({
              key: 'billing_mt_credits_discount',
              defaultValue: 'Discount: {discount, number, :: currency/EUR}',
              parameters: {
                discount: sliderPossibleValues[sliderValue].discount,
              },
            })}
          </Box>

          <LoadingButton
            variant="contained"
            color="primary"
            onClick={() =>
              buy(
                sliderPossibleValues[sliderValue].priceId,
                sliderPossibleValues[sliderValue].itemQuantity
              )
            }
            loading={buyMutation.isLoading}
          >
            Buy
          </LoadingButton>
        </>
      )}
    </>
  );
};
