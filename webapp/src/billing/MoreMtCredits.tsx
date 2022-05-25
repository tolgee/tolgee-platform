import { FC } from 'react';
import {
  useBillingApiMutation,
  useBillingApiQuery,
} from './useBillingQueryApi';
import { Box, Typography } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { useOrganization } from 'tg.views/organizations/useOrganization';
import LoadingButton from 'tg.component/common/form/LoadingButton';

export const MoreMtCredits: FC = () => {
  const organization = useOrganization();

  const pricesLoadable = useBillingApiQuery({
    url: '/v2/billing/mt-credit-prices',
    method: 'get',
  });

  const buyMutation = useBillingApiMutation({
    url: `/v2/organizations/{organizationId}/billing/buy-more-credits/{priceId}`,
    method: 'post',
    options: {
      onSuccess(data) {
        window.location.href = data;
      },
    },
  });

  const t = useTranslate();

  const buy = (priceId: number) => {
    buyMutation.mutate({
      path: {
        priceId: priceId,
        organizationId: organization!.id,
      },
    });
  };

  return (
    <>
      <Typography variant="h3">More credits</Typography>
      {pricesLoadable?.data?._embedded?.prices?.map((price) => (
        <Box key={price.id}>
          {price.amount}:{' '}
          {t({
            key: 'billing_mt_credits_price',
            defaultValue: '{price, number, :: currency/EUR}',
            parameters: { price: price.price },
          })}
          <LoadingButton
            variant="outlined"
            onClick={() => buy(price.id)}
            loading={buyMutation.isLoading}
          >
            Buy
          </LoadingButton>
        </Box>
      ))}
    </>
  );
};
