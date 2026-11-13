import { QUERY } from 'tg.constants/links';
import { useUrlSearchState } from 'tg.hooks/useUrlSearchState';

export type PrefilterType = {
  activity?: number;
  failedJob?: number;
  task?: number;
  taskFilterNotDone?: boolean;
  notification?: number;
  clear: () => void;
};

const stringToNumber = (input: string | undefined) => {
  const asNumber = Number(input);
  if (input !== undefined && !Number.isNaN(asNumber)) {
    return asNumber;
  }
  return undefined;
};

export const usePrefilter = (): PrefilterType | undefined => {
  const [activity, setActivity] = useUrlSearchState(
    QUERY.TRANSLATIONS_PREFILTERS_ACTIVITY,
    {
      defaultVal: undefined,
      history: true,
    }
  );
  const [failedJob, setFailedJob] = useUrlSearchState(
    QUERY.TRANSLATIONS_PREFILTERS_FAILED_JOB,
    {
      defaultVal: undefined,
      history: true,
    }
  );
  const [task, setTask] = useUrlSearchState(
    QUERY.TRANSLATIONS_PREFILTERS_TASK,
    {
      defaultVal: undefined,
      history: true,
    }
  );

  const [notification, setNotification] = useUrlSearchState(
    QUERY.TRANSLATIONS_PREFILTERS_NOTIFICATION,
    {
      defaultVal: undefined,
      history: true,
    }
  );

  const [taskHideDone, setTaskHideDone] = useUrlSearchState(
    QUERY.TRANSLATIONS_PREFILTERS_TASK_HIDE_CLOSED,
    {
      defaultVal: undefined,
    }
  );

  const activityId = stringToNumber(activity);
  const failedJobId = stringToNumber(failedJob);
  const taskNumber = stringToNumber(task);
  const notificationId = stringToNumber(notification);

  function clear() {
    setActivity(undefined);
    setFailedJob(undefined);
    setTask(undefined);
    setTaskHideDone(undefined);
    setNotification(undefined);
  }

  if (activityId !== undefined) {
    return {
      activity: activityId,
      clear,
    };
  } else if (failedJobId !== undefined) {
    return {
      failedJob: failedJobId,
      clear,
    };
  } else if (taskNumber !== undefined) {
    return {
      task: taskNumber,
      taskFilterNotDone: taskHideDone === 'true',
      clear,
    };
  } else if (notificationId !== undefined) {
    return {
      notification: notificationId,
      clear,
    };
  }

  return undefined;
};
