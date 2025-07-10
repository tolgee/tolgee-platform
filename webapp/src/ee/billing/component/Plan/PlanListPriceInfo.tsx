import { PricePrimary } from 'tg.ee.module/billing/component/Price/PricePrimary';
import { styled, useTheme } from '@mui/material';
import { components } from 'tg.service/billingApiSchema.generated';
import { Tooltip } from '@mui/material';
import { T } from '@tolgee/react';

type PlanPricesModel = components['schemas']['PlanPricesModel'];
export type BillingPeriodType =
  components['schemas']['CloudSubscribeRequest']['period'];

const StyledPricePrimary = styled(PricePrimary)<{ bold?: boolean }>`
  font-size: 15px;
  font-weight: ${({ bold }) => (bold ? 'bold' : 'inherit')};
`;

export const PlanListPriceInfo = ({
  prices,
  bold,
}: {
  prices: PlanPricesModel;
  bold?: boolean;
}) => {
  const theme = useTheme();

  const tooltipContent = (
    <div>
      <div>
        <T keyName="administration_cloud_plan_field_price_yearly" />:
        <StyledPricePrimary
          prices={prices}
          period="YEARLY"
          highlightColor={theme.palette.primaryText}
          bold={bold}
          noPeriodSwitch={true}
        />
      </div>
      <div>
        <T keyName="administration_cloud_plan_field_price_monthly" />:
        <StyledPricePrimary
          prices={prices}
          period="MONTHLY"
          highlightColor={theme.palette.primaryText}
          bold={bold}
          noPeriodSwitch={true}
        />
      </div>
    </div>
  );

  return (
    <Tooltip
      title={tooltipContent}
      componentsProps={{ tooltip: { sx: { minWidth: '150px' } } }}
    >
      <span>
        <StyledPricePrimary
          prices={prices}
          period="YEARLY"
          highlightColor={theme.palette.primaryText}
          bold={bold}
          noPeriodSwitch={true}
        />
      </span>
    </Tooltip>
  );
};
