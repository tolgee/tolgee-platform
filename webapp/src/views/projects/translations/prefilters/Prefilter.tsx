import { PrefilterTask } from 'tg.ee';
import { PrefilterActivity } from './PrefilterActivity';
import { PrefilterFailedJob } from './PrefilterFailedJob';
import { PrefilterNotification } from 'tg.views/projects/translations/prefilters/PrefilterNotification';
import { useMarkNotificationSeenOnOpen } from 'tg.views/projects/translations/prefilters/useMarkNotificationSeenOnOpen';
import { PrefilterType } from './usePrefilter';

type Props = {
  prefilter?: PrefilterType;
};

export const Prefilter = ({ prefilter }: Props) => {
  useMarkNotificationSeenOnOpen(prefilter?.notification);

  if (prefilter?.activity) {
    return <PrefilterActivity revisionId={prefilter.activity} />;
  } else if (prefilter?.failedJob) {
    return <PrefilterFailedJob jobId={prefilter.failedJob} />;
  } else if (prefilter?.task) {
    return <PrefilterTask taskNumber={prefilter.task} />;
  } else if (prefilter?.notification) {
    return <PrefilterNotification notificationId={prefilter.notification} />;
  }
  return null;
};
