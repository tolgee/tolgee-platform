import React, { FC } from 'react';
import { components } from 'tg.service/billingApiSchema.generated';
import { Chip } from '@mui/material';
import { SubscriptionRowPlanInfo } from '../generic/SubscriptionRowPlanInfo';
import { T } from '@tolgee/react';

type AdministrationSubscriptionsSelfHostedEeSubscriptionsProps = {
  item: components['schemas']['OrganizationWithSubscriptionsModel'];
};

export const AdministrationSubscriptionsSelfHostedEe: FC<
  AdministrationSubscriptionsSelfHostedEeSubscriptionsProps
> = ({ item }) => {
  return (
    <>
      <SubscriptionRowPlanInfo
        dataCy={'administration-subscriptions-active-self-hosted-ee-plan'}
        label={
          <>
            <T
              keyName={
                'administration-subscriptions-active-self-hosted-ee-plan-cell'
              }
            />
          </>
        }
      >
        <Label item={item} />
      </SubscriptionRowPlanInfo>
    </>
  );
};

const Label: FC<{
  item: components['schemas']['OrganizationWithSubscriptionsModel'];
}> = (props) => {
  const selfHostedSubscriptions = props.item.selfHostedSubscriptions;
  if (selfHostedSubscriptions.length == 0) {
    return <>N/A</>;
  }

  if (selfHostedSubscriptions.length == 1) {
    const item = selfHostedSubscriptions[0];
    return <>{item.plan.name}</>;
  }

  return <Chip size="small" label={selfHostedSubscriptions.length} />;
};
