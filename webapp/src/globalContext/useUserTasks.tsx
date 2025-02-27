import { useApiQuery } from 'tg.service/http/useQueryApi';

import { TASK_ACTIVE_STATES } from 'tg.component/task/taskActiveStates';

export const useUserTasks = (props: { enabled: boolean }) => {
  return useApiQuery({
    url: '/v2/user-tasks',
    method: 'get',
    query: { size: 1, filterState: TASK_ACTIVE_STATES },
    fetchOptions: {
      disableAutoErrorHandle: true,
      disableAuthRedirect: true,
      disableErrorNotification: true,
    },
    options: {
      enabled: props.enabled,
      refetchInterval: 60_000,
      noGlobalLoading: true,
    },
  });
};
