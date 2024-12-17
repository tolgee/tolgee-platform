import { Chip } from '@mui/material';
import { T } from '@tolgee/react';

export function PlanPublicChip({ isPublic }: { isPublic?: boolean }) {
  if (!isPublic) {
    return null;
  }
  return (
    <Chip
      data-cy="administration-cloud-plans-item-public-badge"
      size="small"
      label={<T keyName="administration_cloud_plan_public_badge" />}
    />
  );
}
