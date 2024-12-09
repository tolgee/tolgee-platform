import { Box, styled, SxProps } from '@mui/material';
import { T } from '@tolgee/react';

import { components } from 'tg.service/billingApiSchema.generated';

export type BillingPeriodType =
  components['schemas']['CloudSubscribeRequest']['period'];

const StyledSwitchButton = styled(Box)`
  text-decoration: underline;
  cursor: pointer;
  font-size: 13px;
`;

type Props = {
  value: BillingPeriodType;
  onChange: (period: BillingPeriodType) => void;
  sx?: SxProps;
  className?: string;
};

export const PeriodSwitch: React.FC<Props> = ({
  value,
  onChange,
  sx,
  className,
}) => {
  return (
    <Box
      display="flex"
      justifyContent="center"
      data-cy="billing-period-switch"
      {...{ sx, className }}
    >
      {value === 'MONTHLY' ? (
        <StyledSwitchButton onClick={() => onChange('YEARLY')} role="button">
          <T keyName="billing_period_yearly_switch" />
        </StyledSwitchButton>
      ) : (
        <StyledSwitchButton onClick={() => onChange('MONTHLY')} role="button">
          <T keyName="billing_period_monthly_switch" />
        </StyledSwitchButton>
      )}
    </Box>
  );
};
