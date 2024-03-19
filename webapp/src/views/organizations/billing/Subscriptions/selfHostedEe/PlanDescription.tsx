import { T } from '@tolgee/react';

export const PlanDescription = (props: {
  free: boolean;
  hasPrice: boolean;
}) => {
  if (props.free) {
    return <T keyName="billing_subscriptions_free_plan_description" />;
  }

  if (!props.hasPrice) {
    return <T keyName="billing_subscriptions_pay_for_what_you_use" />;
  }

  return null;
};
