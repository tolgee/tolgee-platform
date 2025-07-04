import { Chip } from '@mui/material';
import { T } from '@tolgee/react';

export function PlanArchivedChip({ isArchived }: { isArchived?: boolean }) {
  if (!isArchived) {
    return null;
  }
  return (
    <Chip
      data-cy="administration-cloud-plans-item-archived-badge"
      size="small"
      label={<T keyName="administration_plan_archived_badge" />}
    />
  );
}
