import { useApiQuery } from 'tg.service/http/useQueryApi';
import { useOrganization } from 'tg.views/organizations/useOrganization';

export const useOrganizationCreditBalance = () => {
  const organization = useOrganization();

  return useApiQuery({
    url: '/v2/organizations/{organizationId}/machine-translation-credit-balance',
    method: 'get',
    path: {
      organizationId: organization!.id,
    },
  });
};
