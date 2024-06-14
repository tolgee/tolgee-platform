import { Box, SxProps, styled } from '@mui/material';
import { T } from '@tolgee/react';
import { components } from 'tg.service/apiSchema.generated';

type PlanPricesModel = components['schemas']['PlanPricesModel'];

const StyledSecondaryPrice = styled(Box)`
  font-size: 13px;
`;

type Props = { prices: PlanPricesModel; sx?: SxProps; className?: string };

export const PayAsYouGoPrices = ({ prices, sx, className }: Props) => {
  const { perSeat, perThousandMtCredits, perThousandTranslations } = prices;

  return (
    <Box display="grid" {...{ sx, className }}>
      {Boolean(perSeat) && (
        <StyledSecondaryPrice data-cy="billing-plan-price-per-seat-extra">
          <T
            keyName="billing-plan-price-per-seat-extra"
            params={{ price: perSeat }}
          />
        </StyledSecondaryPrice>
      )}
      {Boolean(perThousandTranslations) && (
        <StyledSecondaryPrice data-cy="billing-plan-price-per-thousand-strings-extra">
          <T
            keyName="billing-plan-price-per-thousand-strings-extra"
            params={{ price: perThousandTranslations }}
          />
        </StyledSecondaryPrice>
      )}
      {Boolean(perThousandMtCredits) && (
        <StyledSecondaryPrice data-cy="billing-plan-price-per-thousand-mt-credits-extra">
          <T
            keyName="billing-plan-price-per-thousand-mt-credits-extra"
            params={{ price: perThousandMtCredits }}
          />
        </StyledSecondaryPrice>
      )}
    </Box>
  );
};
