import { useEffect, useState } from 'react';
import { components } from 'tg.service/apiSchema.generated';
import { useApiQuery } from 'tg.service/http/useQueryApi';

type OrganizationModel = components['schemas']['OrganizationModel'];
type UsageModel = components['schemas']['PublicUsageModel'];

type Props = {
  organization?: OrganizationModel;
  enabled: boolean;
};

export const useOrganizationUsageService = ({
  organization,
  enabled,
}: Props) => {
  const isOrganizationMember = Boolean(organization?.currentUserRole);
  const [organizationUsage, setOrganizationUsage] = useState<
    UsageModel | undefined
  >(undefined);
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
      onSuccess(data) {
        setOrganizationUsage(data);
      },
    },
  });

  const updateUsageData = (data: Partial<UsageModel>) =>
    setOrganizationUsage((val) =>
      val
        ? {
            ...val,
            ...data,
          }
        : val
    );

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
      usage: organizationUsage,
      planLimitErrors,
      spendingLimitErrors,
    },
    actions: {
      refetchUsage,
      updateUsageData,
      incrementPlanLimitErrors,
      incrementSpendingLimitErrors,
      increaseCreditPlanLimitErrors,
      increaseCreditSpendingLimitErrors,
    },
  };
};
