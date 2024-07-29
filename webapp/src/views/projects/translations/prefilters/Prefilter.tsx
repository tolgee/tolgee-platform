import { PrefilterActivity } from './PrefilterActivity';
import { PrefilterFailedJob } from './PrefilterFailedJob';
import { PrefilterType } from './usePrefilter';

type Props = {
  prefilter?: PrefilterType;
};

export const Prefilter = ({ prefilter }: Props) => {
  if (prefilter?.activity) {
    return <PrefilterActivity revisionId={prefilter.activity} />;
  } else if (prefilter?.failedJob) {
    return <PrefilterFailedJob jobId={prefilter.failedJob} />;
  }
  return null;
};
