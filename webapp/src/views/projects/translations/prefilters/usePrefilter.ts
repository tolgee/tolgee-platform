import { useUrlSearchState } from 'tg.hooks/useUrlSearchState';

export type PrefilterType = {
  activity?: number;
  failedJob?: number;
  task?: number;
  clear: () => void;
};

const stringToNumber = (input: string | undefined) => {
  const asNumber = Number(input);
  if (input !== undefined && !Number.isNaN(asNumber)) {
    return asNumber;
  }
  return undefined;
};

export const usePrefilter = (): PrefilterType => {
  const [activity, setActivity] = useUrlSearchState('activity', {
    defaultVal: undefined,
    history: true,
  });
  const [failedJob, setFailedJob] = useUrlSearchState('failedJob', {
    defaultVal: undefined,
    history: true,
  });
  const [task, setTask] = useUrlSearchState('task', {
    defaultVal: undefined,
    history: true,
  });

  const activityId = stringToNumber(activity);
  const failedJobId = stringToNumber(failedJob);
  const taskNumber = stringToNumber(task);

  function clear() {
    setActivity(undefined);
    setFailedJob(undefined);
    setTask(undefined);
  }

  const result: PrefilterType = {
    clear,
  };

  if (activityId !== undefined) {
    result.activity = activityId;
  } else if (failedJobId !== undefined) {
    result.failedJob = failedJobId;
  } else if (taskNumber !== undefined) {
    result.task = taskNumber;
  }

  return result;
};
