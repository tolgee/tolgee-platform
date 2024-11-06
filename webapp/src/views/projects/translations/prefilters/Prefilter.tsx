import { PrefilterActivity } from './PrefilterActivity';
import { PrefilterFailedJob } from './PrefilterFailedJob';
import { PrefilterTask } from './PrefilterTask';
import { PrefilterType } from './usePrefilter';

type Props = {
  prefilter?: PrefilterType;
};

export const Prefilter = ({ prefilter }: Props) => {
  if (prefilter?.activity) {
    return <PrefilterActivity revisionId={prefilter.activity} />;
  } else if (prefilter?.failedJob) {
    return <PrefilterFailedJob jobId={prefilter.failedJob} />;
  } else if (prefilter?.task) {
    return <PrefilterTask taskNumber={prefilter.task} />;
  }
  return null;
};
