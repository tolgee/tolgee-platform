import { Box, styled } from '@mui/material';
import { T } from '@tolgee/react';

import { components } from 'tg.service/billingApiSchema.generated';

export type BillingPeriodType =
  components['schemas']['SubscribeRequest']['period'];

const StyledSwitchButton = styled(Box)`
  text-decoration: underline;
  cursor: pointer;
  font-size: 13px;
`;

type Props = {
  value: BillingPeriodType;
  onChange: (period: BillingPeriodType) => void;
};

export const PeriodSwitch: React.FC<Props> = ({ value, onChange }) => {
  return (
    <Box
      gridArea="switch"
      display="flex"
      justifyContent="center"
      data-cy="billing-period-switch"
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
