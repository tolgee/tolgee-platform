import { FC } from 'react';
import { useApiQuery } from 'tg.service/http/useQueryApi';

type UsageProps = {
  organizationId: number;
};

export const Usage: FC<UsageProps> = (props) => {
  const usage = useApiQuery({
    url: '/v2/organizations/{organizationId}/usage',
    method: 'get',
    path: {
      organizationId: props.organizationId,
    },
  });

  return <>{JSON.stringify(usage.data)}</>;
};
