import { MenuItem, Select, SelectProps } from '@mui/material';
import { T } from '@tolgee/react';

import { components } from 'tg.service/billingApiSchema.generated';

export type BillingPeriodType =
  components['schemas']['SubscribeRequest']['period'];

type Props = SelectProps<BillingPeriodType>;

export const PeriodSelect: React.FC<Props> = (props) => {
  return (
    <Select {...props}>
      <MenuItem value="MONTHLY">
        <T keyName="billing_period_monthly" />
      </MenuItem>
      <MenuItem value="YEARLY">
        <T keyName="billing_period_yearly" />
      </MenuItem>
    </Select>
  );
};
