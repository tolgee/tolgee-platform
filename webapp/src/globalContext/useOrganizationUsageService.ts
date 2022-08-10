import { useEffect, useState } from 'react';
import { components } from 'tg.service/apiSchema.generated';
import { useApiQuery } from 'tg.service/http/useQueryApi';

type OrganizationModel = components['schemas']['OrganizationModel'];
type UsageModel = components['schemas']['UsageModel'];

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

  const usageEnabled =
    organization?.id !== undefined && enabled && isOrganizationMember;

  const usageLoadable = useApiQuery({
    url: '/v2/organizations/{organizationId}/usage',
    method: 'get',
    path: {
      organizationId: organization?.id || 0,
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

  const updateData = (data: Partial<UsageModel>) =>
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

  const refetch = () => {
    if (usageEnabled) {
      usageLoadable.refetch();
    }
  };

  useEffect(() => {
    if (planLimitErrors) {
      refetch();
    }
  }, [planLimitErrors]);

  return {
    data: {
      usage: organizationUsage,
      planLimitErrors,
    },
    refetch,
    updateData,
    incrementPlanLimitErrors,
  };
};
