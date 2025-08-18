import { components } from 'tg.service/billingApiSchema.generated';
import { Chip, Tooltip } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { useDateFormatter } from 'tg.hooks/useLocale';

type Status = components['schemas']['PlanMigrationHistoryModel']['status'];

type Props = {
  status: Status;
  date?: number;
};

const colors = {
  COMPLETED: 'success',
};

const translates = {
  COMPLETED: 'administration_plan_migration_status_completed',
  SCHEDULED: 'administration_plan_migration_status_scheduled',
};

export const PlanMigrationStatus = ({ status, date }: Props) => {
  const { t } = useTranslate();
  const formatDate = useDateFormatter();

  const chip = (
    <Chip label={t(translates[status])} color={colors[status] || 'default'} />
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
