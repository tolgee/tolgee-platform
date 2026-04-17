import { FC } from 'react';
import { Box } from '@mui/material';
import { T } from '@tolgee/react';

import { components as billingComponents } from 'tg.service/billingApiSchema.generated';
import { useDateFormatter } from 'tg.hooks/useLocale';
import { Alert } from 'tg.component/common/Alert';
import { useCurrentDate } from 'tg.hooks/useCurrentDate';

type CloudSubscriptionModel =
  billingComponents['schemas']['CloudSubscriptionModel'];

type PlanMigrationAlertData = {
  variant: 'scheduled' | 'completed';
  from: string;
  to: string;
  date?: number;
};

const getPeriodDuration = (subscription: CloudSubscriptionModel) => {
  if (
    subscription.currentPeriodStart &&
    subscription.currentPeriodEnd &&
    subscription.currentPeriodEnd > subscription.currentPeriodStart
  ) {
    return subscription.currentPeriodEnd - subscription.currentPeriodStart;
  }

  // 14 days
  return 1000 * 60 * 60 * 24 * 14;
};

const getPlanMigrationAlertData = (
  subscription: CloudSubscriptionModel,
  currentDate: Date
): PlanMigrationAlertData | undefined => {
  const migration = subscription.planMigration;

  if (!migration) {
    return undefined;
  }

  const targetPlanName = migration.targetPlanName || subscription.plan.name;
  const now = currentDate.getTime();

  if (
    migration.status === 'SCHEDULED' &&
    migration.scheduledAt &&
    migration.scheduledAt >= now &&
    !subscription.cancelAtPeriodEnd
  ) {
    return {
      variant: 'scheduled',
      from: migration.originPlanName,
      to: targetPlanName,
      date: subscription.currentPeriodEnd ?? undefined,
    };
  }

  if (
    migration.status === 'COMPLETED' &&
    migration.finalizedAt &&
    migration.finalizedAt <= now
  ) {
    const periodDuration = getPeriodDuration(subscription);
    if (now - migration.finalizedAt < periodDuration) {
      return {
        variant: 'completed',
        from: migration.originPlanName,
        to: targetPlanName,
        date: migration.finalizedAt,
      };
    }
  }

  return undefined;
};

type Props = {
  subscription: CloudSubscriptionModel;
};

export const PlanMigrationAlert: FC<Props> = ({ subscription }) => {
  const formatDate = useDateFormatter();
  const currentDate = useCurrentDate();
  const migrationAlert = getPlanMigrationAlertData(subscription, currentDate);

  if (!migrationAlert) {
    return null;
  }

  const baseParams = {
    from: migrationAlert.from,
    to: migrationAlert.to,
    b: <b />,
  };

  const getScheduledContent = () => {
    if (migrationAlert.date) {
      return (
        <T
          keyName="billing_plan_migration_alert_scheduled"
          params={{
            ...baseParams,
            date: formatDate(migrationAlert.date, { dateStyle: 'long' }),
          }}
        />
      );
    }
    return (
      <T
        keyName="billing_plan_migration_alert_scheduled_no_date"
        params={baseParams}
      />
    );
  };

  return (
    <Box display="flex" mb={1}>
      <Alert severity="info" data-cy="billing-plan-migration-alert">
        {migrationAlert.variant === 'scheduled' ? (
          getScheduledContent()
        ) : (
          <T
            keyName="billing_plan_migration_alert_completed"
            params={{
              ...baseParams,
              date: formatDate(migrationAlert.date!, { dateStyle: 'long' }),
            }}
          />
        )}
      </Alert>
    </Box>
  );
};
