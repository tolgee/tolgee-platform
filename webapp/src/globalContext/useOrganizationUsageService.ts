import { useEffect, useState } from 'react';
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
  const [planLimitErrors, setPlanLimitErrors] = useState(0);
  const [spendingLimitErrors, setSpendingLimitErrors] = useState(0);

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

  const usage = usageEnabled ? usageLoadable.data : undefined;

  const incrementPlanLimitErrors = () => {
    setPlanLimitErrors((v) => v + 1);
  };

  const incrementSpendingLimitErrors = () => {
    setSpendingLimitErrors((v) => v + 1);
  };

  /**
   * For MT credit error, we want to show the error only once.
   * We don't want to disturb the translators that much with the error.
   */
  const increaseCreditPlanLimitErrors = () => {
    setPlanLimitErrors((v) => {
      if (v > 0) {
        return v;
      }
      return v + 1;
    });
  };

  /**
   * For MT credit error, we want to show the error only once.
   * We don't want to disturb the translators that much with the error.
   */
  const increaseCreditSpendingLimitErrors = () => {
    setSpendingLimitErrors((v) => {
      if (v > 0) {
        return v;
      }
      return v + 1;
    });
  };

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
      usage,
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
