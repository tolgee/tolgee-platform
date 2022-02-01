import { useSelector } from 'react-redux';

import { useApiQuery } from '../service/http/useQueryApi';
import { UserDTO } from '../service/response.types';
import { AppState } from '../store';
import { components } from 'tg.service/apiSchema.generated';

export const useUser = (): components['schemas']['UserAccountModel'] | null => {
  const jwt = useSelector((state: AppState) => state.global.security.jwtToken);

  const { data, error } = useApiQuery({
    url: '/v2/user',
    method: 'get',
    options: {
      enabled: Boolean(jwt),
    },
  });

  const allowPrivate = useSelector(
    (state: AppState) => state.global.security.allowPrivate
  );

  if (error) {
    throw error;
  }

  if (!allowPrivate) {
    return null;
  }

  return data as UserDTO | null;
};
