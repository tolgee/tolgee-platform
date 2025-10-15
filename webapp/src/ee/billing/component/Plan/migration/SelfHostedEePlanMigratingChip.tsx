import { useBillingApiQuery } from 'tg.service/http/useQueryApi';
import React, { useState } from 'react';
import { LINKS } from 'tg.constants/links';
import { PlanMigrationChip } from 'tg.ee.module/billing/component/Plan/migration/PlanMigrationChip';

export const SelfHostedEePlanMigratingChip = ({
  migrationId,
  isEnabled,
}: {
  migrationId?: number;
  isEnabled?: boolean;
}) => {
  const [opened, setOpened] = useState(false);
  const loadable = useBillingApiQuery({
    url: '/v2/administration/billing/self-hosted-ee-plans/migration/{migrationId}',
    method: 'get',
    path: { migrationId: migrationId! },
    options: {
      enabled: !!migrationId && opened,
    },
  });

  if (!migrationId) {
    return null;
  }

  return (
    <PlanMigrationChip
      loadable={loadable}
      editLink={LINKS.ADMINISTRATION_BILLING_EE_PLAN_MIGRATION_EDIT}
      isEnabled={isEnabled}
      onOpen={() => setOpened(true)}
    />
  );
};
