import { ListItemText } from '@mui/material';
import { T } from '@tolgee/react';

export const PlanSubscriptionCount = ({ count }: { count: number }) => {
  if (count == 0) {
    return null;
  }
  return (
    <ListItemText>
      <T
        keyName="administration_subscription_count"
        params={{ count: count }}
      />
    </ListItemText>
  );
};
