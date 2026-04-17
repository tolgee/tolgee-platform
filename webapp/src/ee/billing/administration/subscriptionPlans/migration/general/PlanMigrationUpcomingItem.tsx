import {
  Checkbox,
  Link,
  styled,
  TableCell,
  TableRow,
  Typography,
} from '@mui/material';
import { Link as RouterLink } from 'react-router-dom';

import { PlanMigrationStatus } from './PlanMigrationStatus';
import { LINKS, PARAMS } from 'tg.constants/links';
import { components } from 'tg.service/billingApiSchema.generated';
import { FormatedDateTooltip } from 'tg.component/common/tooltip/FormatedDateTooltip';
import React from 'react';
import { useDateFormatter, useMoneyFormatter } from 'tg.hooks/useLocale';
import { T } from '@tolgee/react';

type UpcomingItem =
  components['schemas']['PlanMigrationUpcomingSubscriptionModel'];

type Props = {
  subscription: UpcomingItem;
  onToggleSkip: (subscriptionId: number, skipped: boolean) => void;
  toggleLoading?: boolean;
};

const StyledTableRow = styled(TableRow)<{ skipped: boolean }>`
  .MuiTableCell-body {
    color: ${({ skipped, theme }) =>
      skipped ? theme.palette.text.disabled : 'inherit'};
  }
`;

export const PlanMigrationUpcomingItem = ({
  subscription,
  onToggleSkip,
  toggleLoading,
}: Props) => {
  const formatDate = useDateFormatter();
  const formatMoney = useMoneyFormatter();

  return (
    <StyledTableRow skipped={subscription.skipped}>
      <TableCell data-cy="plan-migration-upcoming-list-item">
        <Link
          component={RouterLink}
          to={LINKS.ORGANIZATION_PROFILE.build({
            [PARAMS.ORGANIZATION_SLUG]: subscription.organizationSlug,
          })}
        >
          <Typography>{subscription.organizationName}</Typography>
        </Link>
        <Typography variant="caption">
          {subscription.firstPaymentDate && (
            <T
              keyName="administration_plan_migration_first_payment_date"
              params={{
                date: formatDate(subscription.firstPaymentDate, {
                  dateStyle: 'long',
                }),
              }}
            />
          )}
        </Typography>
      </TableCell>
      <TableCell>{subscription.originPlan}</TableCell>
      <TableCell>{subscription.targetPlan}</TableCell>
      <TableCell>{formatMoney(subscription.expectedUsage.total)}</TableCell>
      <TableCell>
        <FormatedDateTooltip date={subscription.scheduleAt} />
      </TableCell>
      <TableCell>
        <Checkbox
          color="primary"
          checked={Boolean(subscription.skipped)}
          onChange={(e) =>
            onToggleSkip(subscription.subscriptionId, e.target.checked)
          }
          disabled={toggleLoading}
          data-cy="plan-migration-subscription-skip"
        />
      </TableCell>
      <TableCell>
        <PlanMigrationStatus
          status={subscription.skipped ? 'SKIPPED' : 'TO_BE_SCHEDULED'}
        />
      </TableCell>
    </StyledTableRow>
  );
};
