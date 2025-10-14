import { useBillingApiQuery } from 'tg.service/http/useQueryApi';
import React, { useState } from 'react';
import { LINKS } from 'tg.constants/links';
import { PlanMigrationChip } from 'tg.ee.module/billing/component/Plan/migration/PlanMigrationChip';

export const CloudPlanMigratingChip = ({
  migrationId,
  isEnabled,
}: {
  migrationId?: number;
  isEnabled?: boolean;
}) => {
  if (!migrationId) {
    return null;
  }
  const [opened, setOpened] = useState(false);
  const loadable = useBillingApiQuery({
    url: '/v2/administration/billing/cloud-plans/migration/{migrationId}',
    method: 'get',
    path: { migrationId: migrationId },
    options: {
      enabled: !!migrationId && opened,
    },
  });

  return (
    <PlanMigrationChip
      loadable={loadable}
      editLink={LINKS.ADMINISTRATION_BILLING_CLOUD_PLAN_MIGRATION_EDIT}
      isEnabled={isEnabled}
      onOpen={() => setOpened(true)}
    />
  );
};
