import { UseQueryOptions } from 'react-query';
import { useApiQuery } from 'tg.service/http/useQueryApi';

type Props = UseQueryOptions<any> & {
  organizationId: number;
};

export const useBillingUsageData = ({ organizationId, ...options }: Props) => {
  const usage = useApiQuery({
    url: '/v2/organizations/{organizationId}/usage',
    method: 'get',
    path: {
      organizationId,
    },
    options: {
      cacheTime: 1000 * 60 * 60,
      staleTime: 1000 * 30,
      ...options,
    },
  });

  return usage;
};
