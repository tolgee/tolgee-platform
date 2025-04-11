import { useApiQuery } from 'tg.service/http/useQueryApi';
import { useUrlSearch } from 'tg.hooks/useUrlSearch';

/**
 * Helper hook providing information about organization which we create a custom plan for.
 *
 * From Subscriptions page, we can create a custom plan for an organization for both Self-hosted or Cloud plans.
 *
 * This hook provides parses the URL query, and provides data required for this functionality.
 */
export const useCreatingForOrganization = ({
  initialPlanName,
}: {
  initialPlanName: string;
}) => {
  const { creatingForOrganizationId: creatingForOrganizationIdString } =
    useUrlSearch();

  const creatingForOrganizationId = creatingForOrganizationIdString
    ? parseInt(creatingForOrganizationIdString as string)
    : undefined;

  const organizationLoadable = useApiQuery({
    url: '/v2/organizations/{id}',
    method: 'get',
    path: { id: creatingForOrganizationId || 0 },
    options: {
      enabled: !!creatingForOrganizationId,
    },
  });

  function getInitialPlanName() {
    if (!initialPlanName && organizationLoadable.data?.name) {
      return 'Custom for ' + organizationLoadable.data.name;
    }
    return initialPlanName;
  }

  return {
    id: organizationLoadable.data?.id,
    data: organizationLoadable.data,
    initialPlanName: getInitialPlanName(),
  };
};
