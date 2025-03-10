import { Box, styled, SxProps } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { components } from 'tg.service/apiSchema.generated';
import { PayAsYouGoRow } from './PayAsYouGoRow';
import { useMoneyFormatter } from 'tg.hooks/useLocale';

type PlanPricesModel = components['schemas']['PlanPricesModel'];

const StyledTitle = styled(Box)`
  text-align: center;
  font-weight: 500;
`;

type Props = {
  prices: PlanPricesModel;
  sx?: SxProps;
  className?: string;
  hideTitle?: boolean;
};

export const PayAsYouGoPrices = ({
  prices,
  sx,
  className,
  hideTitle,
}: Props) => {
  const {
    perSeat,
    perThousandMtCredits,
    perThousandTranslations,
    perThousandKeys,
  } = prices;
  const { t } = useTranslate();
  const formatPrice = useMoneyFormatter();

  const display = perSeat || perThousandMtCredits || perThousandTranslations;

  if (!display) {
    return null;
  }

  return (
    <Box display="grid" gap="2px" {...{ sx, className }}>
      {!hideTitle && (
        <StyledTitle sx={{ paddingBottom: '2px' }}>
          <T keyName="billing-plan-pay-as-you-go-price" />
        </StyledTitle>
      )}

      {Boolean(perThousandTranslations) && (
        <PayAsYouGoRow
          data-cy="billing-plan-price-extra-thousand-strings"
          firstPart={t('billing-plan-price-extra-thousand-strings', {
            num: 1000,
          })}
          secondPart={t('billing-plan-price-per-mo', {
            value: perThousandTranslations,
          })}
        />
      )}

      {Boolean(perThousandKeys) && (
        <PayAsYouGoRow
          data-cy="billing-plan-price-extra-thousand-keys"
          firstPart={t('billing-plan-price-extra-thousand-keys', { num: 1000 })}
          secondPart={t('billing-plan-price-per-mo', {
            value: perThousandKeys,
          })}
        />
      )}

      {Boolean(perSeat) && (
        <PayAsYouGoRow
          data-cy="billing-plan-price-extra-seat"
          firstPart={t('billing-plan-price-extra-seat')}
          secondPart={t('billing-plan-price-per-mo', { value: perSeat })}
        />
      )}

      {Boolean(perThousandMtCredits) && (
        <PayAsYouGoRow
          data-cy="billing-plan-price-extra-thousand-mt-credits"
          firstPart={t('billing-plan-price-extra-thousand-mt-credits', {
            num: 1000,
          })}
          secondPart={formatPrice(perThousandMtCredits, {
            maximumFractionDigits: 4,
          })}
        />
      )}
    </Box>
  );
};
