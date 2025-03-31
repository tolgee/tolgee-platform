import React, { FC, useState } from 'react';
import { components } from 'tg.service/billingApiSchema.generated';
import { Chip } from '@mui/material';
import { SubscriptionRowPlanInfo } from '../generic/SubscriptionRowPlanInfo';
import { T } from '@tolgee/react';
import { SubscriptionsSelfHostedPopover } from './SubscriptionsSelfHostedPopover';
import { AssignSelfHostedPlanDialog } from './AssignSelfHostedPlanDialog';

type AdministrationSubscriptionsSelfHostedEeSubscriptionsProps = {
  item: components['schemas']['OrganizationWithSubscriptionsModel'];
};

export const AdministrationSubscriptionsSelfHostedEe: FC<
  AdministrationSubscriptionsSelfHostedEeSubscriptionsProps
> = ({ item }) => {
  const [assignPlanDialogOpen, setAssignPlanDialogOpen] = useState(false);

  return (
    <>
      <SubscriptionsSelfHostedPopover
        item={item}
        onOpenAssignTrialDialog={() => setAssignPlanDialogOpen(true)}
      >
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
      </SubscriptionsSelfHostedPopover>
      <AssignSelfHostedPlanDialog
        open={assignPlanDialogOpen}
        onClose={() => setAssignPlanDialogOpen(false)}
        item={item}
      />
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
