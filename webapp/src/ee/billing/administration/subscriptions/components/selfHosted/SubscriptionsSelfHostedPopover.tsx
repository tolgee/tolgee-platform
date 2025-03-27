import React, { FC } from 'react';
import { SubscriptionsDetailPopover } from '../generic/SubscriptionsDetailPopover';
import { components } from 'tg.service/billingApiSchema.generated';
import { SubscriptionCurrentPlanInfo } from '../generic/SubscriptionCurrentPlanInfo';
import { SubscriptionsSelfHostedEditPlanButton } from './SubscriptionsSelfHostedEditPlanButton';

type SubscriptionsSelfHostedPopoverProps = {
  item: components['schemas']['OrganizationWithSubscriptionsModel'];
  children: React.ReactElement;
};

export const SubscriptionsSelfHostedPopover: FC<
  SubscriptionsSelfHostedPopoverProps
> = ({ item, children }) => {
  return (
    <SubscriptionsDetailPopover
      popoverContent={
        <>
          {item.selfHostedSubscriptions.map((i) => (
            <SubscriptionCurrentPlanInfo
              key={i.id}
              subscription={i}
              editButton={
                <SubscriptionsSelfHostedEditPlanButton
                  subscription={i}
                  organizationId={item.organization.id}
                />
              }
            />
          ))}
        </>
      }
    >
      <span>{children}</span>
    </SubscriptionsDetailPopover>
  );
};
