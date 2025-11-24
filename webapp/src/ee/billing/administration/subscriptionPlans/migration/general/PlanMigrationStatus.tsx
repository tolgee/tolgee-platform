import { components } from 'tg.service/billingApiSchema.generated';
import { Chip, Tooltip } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { useDateFormatter } from 'tg.hooks/useLocale';

type Status =
  | components['schemas']['PlanMigrationRecordModel']['status']
  | 'TO_BE_SCHEDULED';

type Props = {
  status: Status;
  date?: number;
};

const colors = {
  COMPLETED: 'success',
  SKIPPED: 'default',
  TO_BE_SCHEDULED: 'info',
};

export const PlanMigrationStatus = ({ status, date }: Props) => {
  const { t } = useTranslate();
  const formatDate = useDateFormatter();

  const getStatusLabel = (s: Status): string => {
    switch (s) {
      case 'COMPLETED':
        return t('administration_plan_migration_status_completed');
      case 'SCHEDULED':
        return t('administration_plan_migration_status_scheduled');
      case 'SKIPPED':
        return t('administration_plan_migration_status_skipped');
      case 'TO_BE_SCHEDULED':
        return t('administration_plan_migration_status_to_be_scheduled');
      default:
        return String(s);
    }
  };

  const chip = (
    <Chip label={getStatusLabel(status)} color={colors[status] || 'default'} />
  );

  return date ? (
    <Tooltip
      title={formatDate(date, {
        timeZone: 'UTC',
        dateStyle: 'short',
        timeStyle: 'short',
      })}
    >
      {chip}
    </Tooltip>
  ) : (
    chip
  );
};
