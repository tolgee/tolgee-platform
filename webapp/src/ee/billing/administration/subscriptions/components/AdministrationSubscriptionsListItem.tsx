import {
  Chip,
  Link,
  ListItem,
  ListItemSecondaryAction,
  ListItemText,
} from '@mui/material';
import { LINKS, PARAMS } from 'tg.constants/links';
import { AdministrationSubscriptionsCloudPlan } from './AdministrationSubscriptionsCloudPlan';
import React, { FC } from 'react';
import { components } from 'tg.service/billingApiSchema.generated';

export const AdministrationSubscriptionsListItem: FC<{
  item: components['schemas']['OrganizationWithSubscriptionsModel'];
}> = ({ item }) => {
  return (
    <ListItem data-cy="administration-organizations-list-item">
      <ListItemText>
        <Link
          href={LINKS.ORGANIZATION_PROFILE.build({
            [PARAMS.ORGANIZATION_SLUG]: item.organization.slug,
          })}
        >
          {item.organization.name}
        </Link>{' '}
        <Chip size="small" label={item.organization.id} />
        <AdministrationSubscriptionsCloudPlan item={item} />
      </ListItemText>
      <ListItemSecondaryAction></ListItemSecondaryAction>
    </ListItem>
  );
};
