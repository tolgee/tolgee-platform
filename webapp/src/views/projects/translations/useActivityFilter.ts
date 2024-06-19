import { useUrlSearchState } from 'tg.hooks/useUrlSearchState';

export const useActivityFilter = () => {
  const [activity, setActivity] = useUrlSearchState('activity', {
    defaultVal: undefined,
    history: true,
  });
  const activityId = activity === undefined ? undefined : Number(activity);

  function clear() {
    setActivity(undefined);
  }

  return {
    revisionId: Number.isNaN(activityId) ? undefined : activityId,
    clear,
  };
};
