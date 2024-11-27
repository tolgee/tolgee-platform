import { useApiQuery } from 'tg.service/http/useQueryApi';
import { TASK_ACTIVE_STATES } from 'tg.ee/task/components/utils';

export const useUserTasks = (props: { enabled: boolean }) => {
  // const [userTasks, setUserTasks] = useState(0);

  const userTasksLoadable = useApiQuery({
    url: '/v2/user-tasks',
    method: 'get',
    query: { size: 1, filterState: TASK_ACTIVE_STATES },
    options: {
      enabled: props.enabled,
      refetchInterval: 60_000,
    },
  });

  // useEffect(() => {
  //   setUserTasks(userTasksLoadable.data?.page?.totalElements ?? 0);
  // }, [userTasksLoadable.data]);

  return userTasksLoadable;
};
