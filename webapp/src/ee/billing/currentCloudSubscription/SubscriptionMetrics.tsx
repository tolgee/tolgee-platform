import { FC } from 'react';
import { T } from '@tolgee/react';
import { PlanMetric } from './PlanMetric';
import { MtHint } from 'tg.component/billing/MtHint';
import {
  KeysHint,
  SeatsHint,
  StringsHint,
} from 'tg.component/common/StringsHint';
import { ProgressData } from '../component/getProgressData';
import { MetricType } from '../administration/subscriptionPlans/components/planForm/cloud/types';

type Props = {
  metricType: MetricType;
  progressData: Partial<ProgressData>;
  isPayAsYouGo: boolean;
};

export const SubscriptionMetrics: FC<Props> = ({
  metricType,
  progressData,
  isPayAsYouGo,
}) => {
  return (
    <>
      {metricType === 'STRINGS' &&
        progressData.stringsProgress &&
        progressData.stringsProgress.included > -1 && (
          <PlanMetric
            name={
              <T
                keyName="billing_actual_used_strings_with_hint"
                params={{ hint: <StringsHint /> }}
              />
            }
            progress={progressData.stringsProgress}
            isPayAsYouGo={isPayAsYouGo}
            data-cy="billing-actual-used-strings"
          />
        )}

      {metricType === 'KEYS_SEATS' &&
        progressData.keysProgress &&
        progressData.keysProgress.included > -1 && (
          <PlanMetric
            name={
              <T
                keyName="billing_actual_used_keys_with_hint"
                params={{ hint: <KeysHint /> }}
              />
            }
            progress={progressData.keysProgress}
            isPayAsYouGo={isPayAsYouGo}
            data-cy="billing-actual-used-keys"
          />
        )}

      {metricType === 'KEYS_SEATS' &&
        progressData.seatsProgress &&
        progressData.seatsProgress.included > -1 && (
          <PlanMetric
            name={
              <T
                keyName="billing_actual_used_seats_with_hint"
                params={{ hint: <SeatsHint /> }}
              />
            }
            progress={progressData.seatsProgress}
            isPayAsYouGo={isPayAsYouGo}
            data-cy="billing-actual-used-seats"
          />
        )}

      {progressData.creditProgress &&
        progressData.creditProgress.included > -1 && (
          <PlanMetric
            name={
              <T
                keyName="billing_actual_used_monthly_credits"
                params={{ hint: <MtHint /> }}
              />
            }
            progress={progressData.creditProgress}
            isPayAsYouGo={isPayAsYouGo}
            data-cy="billing-actual-used-monthly-credits"
          />
        )}
    </>
  );
};
