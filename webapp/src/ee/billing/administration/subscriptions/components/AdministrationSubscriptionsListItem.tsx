import { Chip, Link, TableCell, TableRow } from '@mui/material';
import { LINKS, PARAMS } from 'tg.constants/links';
import { AdministrationSubscriptionsCloudPlan } from './cloudPlan/AdministrationSubscriptionsCloudPlan';
import React, { FC } from 'react';
import { components } from 'tg.service/billingApiSchema.generated';
import { AdministrationSubscriptionsSelfHostedEe } from './selfHosted/AdministrationSubscriptionsSelfHostedEe';

export const AdministrationSubscriptionsListItem: FC<{
  item: components['schemas']['OrganizationWithSubscriptionsModel'];
}> = ({ item }) => {
  return (
    <TableRow data-cy="administration-organizations-list-item">
      <TableCell
        sx={{
          minWidth: 0,
          width: '1%',
          whiteSpace: 'nowrap',
        }}
      >
        <Link
          href={LINKS.ORGANIZATION_PROFILE.build({
            [PARAMS.ORGANIZATION_SLUG]: item.organization.slug,
          })}
        >
          {item.organization.name}
        </Link>{' '}
        <Chip size="small" label={item.organization.id} />
      </TableCell>
      <TableCell>
        <AdministrationSubscriptionsCloudPlan item={item} />
      </TableCell>
      <TableCell>
        <AdministrationSubscriptionsSelfHostedEe item={item} />
      </TableCell>
    </TableRow>
  );
};
