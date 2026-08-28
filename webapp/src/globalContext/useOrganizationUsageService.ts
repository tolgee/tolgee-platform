import { useEffect, useState } from 'react';

import {
  LimitErrorEvent,
  limitErrorCounters,
  noLimitErrors,
} from 'tg.fixtures/limitErrorCounters';
import { components } from 'tg.service/apiSchema.generated';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { isAtLeastMemberOrgRole } from 'tg.fixtures/organizationRole';

type OrganizationModel = components['schemas']['OrganizationModel'];

type Props = {
  organization?: OrganizationModel;
  enabled: boolean;
};

export const useOrganizationUsageService = ({
  organization,
  enabled,
}: Props) => {
  const isOrganizationMember = isAtLeastMemberOrgRole(
    organization?.currentUserRole
  );
  const [counters, setCounters] = useState(() => noLimitErrors());
  const { planLimitErrors, spendingLimitErrors } = counters;

  const usageEnabled =
    organization?.id !== undefined && enabled && isOrganizationMember;

  const usageLoadable = useApiQuery({
    url: '/v2/organizations/{organizationId}/usage',
    method: 'get',
    path: {
      organizationId: organization?.id || 0,
    },
    fetchOptions: {
      disableAutoErrorHandle: true,
      disableAuthRedirect: true,
      disableErrorNotification: true,
    },
    options: {
      refetchOnMount: false,
      cacheTime: Infinity,
      enabled: usageEnabled,
    },
  });

  useEffect(() => {
    setCounters((state) =>
      limitErrorCounters(state, { kind: 'organization-changed' })
    );
  }, [organization?.id]);

  const organizationUsage = usageLoadable.data;

  const report = (event: LimitErrorEvent) =>
    setCounters((state) => limitErrorCounters(state, event));

  const incrementPlanLimitErrors = () => report({ kind: 'plan-limit' });

  const incrementSpendingLimitErrors = () => report({ kind: 'spending-limit' });

  const increaseCreditPlanLimitErrors = () =>
    report({ kind: 'credit-plan-limit' });

  const increaseCreditSpendingLimitErrors = () =>
    report({ kind: 'credit-spending-limit' });

  const refetchUsage = () => {
    if (usageEnabled) {
      usageLoadable.refetch();
    }
  };

  useEffect(() => {
    if (planLimitErrors || spendingLimitErrors) {
      refetchUsage();
    }
  }, [planLimitErrors, spendingLimitErrors]);

  return {
    state: {
      usage: organizationUsage,
      planLimitErrors,
      spendingLimitErrors,
    },
    actions: {
      refetchUsage,
      incrementPlanLimitErrors,
      incrementSpendingLimitErrors,
      increaseCreditPlanLimitErrors,
      increaseCreditSpendingLimitErrors,
    },
  };
};
