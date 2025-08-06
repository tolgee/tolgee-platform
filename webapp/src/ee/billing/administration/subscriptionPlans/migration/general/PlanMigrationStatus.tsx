import { components } from 'tg.service/billingApiSchema.generated';
import { Chip } from '@mui/material';
import { useTranslate } from '@tolgee/react';

type Status = components['schemas']['PlanMigrationHistoryModel']['status'];

type Props = {
  status: Status;
};

const colors = {
  COMPLETED: 'success',
};

const translates = {
  COMPLETED: 'administration_plan_migration_status_completed',
  SCHEDULED: 'administration_plan_migration_status_scheduled',
};

export const PlanMigrationStatus = ({ status }: Props) => {
  const { t } = useTranslate();
  return (
    <Chip label={t(translates[status])} color={colors[status] || 'default'} />
  );
};
