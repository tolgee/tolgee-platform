import { T } from '@tolgee/react';
import { PlanFeature } from '../PlanFeature';

type Props = {
  planName: string;
};

export const AllFromPlanFeature = ({ planName }: Props) => {
  return (
    <PlanFeature
      bold
      name={
        <T
          keyName="billing_subscriptions_all_from_plan_label"
          params={{ name: planName }}
        />
      }
    />
  );
};
