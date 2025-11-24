import { Checkbox, Link, styled, TableCell, TableRow } from '@mui/material';
import { Link as RouterLink } from 'react-router-dom';

import { PlanMigrationStatus } from './PlanMigrationStatus';
import { LINKS, PARAMS } from 'tg.constants/links';
import { components } from 'tg.service/billingApiSchema.generated';
import { FormatedDateTooltip } from 'tg.component/common/tooltip/FormatedDateTooltip';

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
  return (
    <StyledTableRow skipped={subscription.skipped}>
      <TableCell>
        <Link
          component={RouterLink}
          to={LINKS.ORGANIZATION_PROFILE.build({
            [PARAMS.ORGANIZATION_SLUG]: subscription.organizationSlug,
          })}
        >
          {subscription.organizationName}
        </Link>
      </TableCell>
      <TableCell>{subscription.originPlan}</TableCell>
      <TableCell>{subscription.targetPlan}</TableCell>
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
