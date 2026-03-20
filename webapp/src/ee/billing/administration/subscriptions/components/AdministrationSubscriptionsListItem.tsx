import { Chip, Link, TableCell, TableRow, Typography } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { LINKS, PARAMS } from 'tg.constants/links';
import { AdministrationSubscriptionsCloudPlan } from './cloudPlan/AdministrationSubscriptionsCloudPlan';
import React, { FC } from 'react';
import { components } from 'tg.service/billingApiSchema.generated';
import { AdministrationSubscriptionsSelfHostedEe } from './selfHosted/AdministrationSubscriptionsSelfHostedEe';

export const AdministrationSubscriptionsListItem: FC<{
  item: components['schemas']['OrganizationWithSubscriptionsModel'];
}> = ({ item }) => {
  const { t } = useTranslate();

  return (
    <TableRow
      data-cy="administration-organizations-list-item"
      sx={item.deleted ? { opacity: 0.6 } : undefined}
    >
      <TableCell
        sx={{
          minWidth: 0,
          width: '1%',
          whiteSpace: 'nowrap',
        }}
      >
        {item.deleted ? (
          <Typography component="span" color="text.secondary">
            {item.organization.name}
          </Typography>
        ) : (
          <Link
            href={LINKS.ORGANIZATION_PROFILE.build({
              [PARAMS.ORGANIZATION_SLUG]: item.organization.slug,
            })}
          >
            {item.organization.name}
          </Link>
        )}{' '}
        <Chip size="small" label={item.organization.id} />
        {item.deleted && (
          <Chip
            size="small"
            label={t('administration_subscriptions_deleted', 'Deleted')}
            color="error"
            sx={{ ml: 0.5 }}
          />
        )}
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
