import React from 'react';
import { T } from '@tolgee/react';

import { useNumberFormatter } from 'tg.hooks/useLocale';
import { components } from 'tg.service/billingApiSchema.generated';
import { MtHint } from 'tg.component/billing/MtHint';
import { StringsHint } from 'tg.component/billing/StringsHint';

import { PlanMetrics } from '../../common/PlanMetrics';

type PlanModel = components['schemas']['CloudPlanModel'];

type Props = {
  plan: PlanModel;
};

export const CloudPlanInfo: React.FC<Props> = ({ plan }) => {
  const usesSlots = plan.type === 'SLOTS_FIXED';
  const formatNumber = useNumberFormatter();

  return (
    <PlanMetrics
      left={{
        number: formatNumber(
          usesSlots
            ? plan.includedUsage.translationSlots
            : plan.includedUsage.translations!
        ),
        name:
          plan.type === 'PAY_AS_YOU_GO' ? (
            <T
              keyName="billing_plan_strings_included_with_hint"
              params={{ hint: <StringsHint /> }}
            />
          ) : plan.type === 'SLOTS_FIXED' ? (
            <T keyName="billing_plan_translation_limit" />
          ) : (
            <T
              keyName="billing_plan_strings_limit_with_hint"
              params={{ hint: <StringsHint /> }}
            />
          ),
      }}
      right={{
        number: formatNumber(plan.includedUsage.mtCredits || 0),
        name: (
          <T
            keyName="billing_plan_credits_included"
            params={{ hint: <MtHint /> }}
          />
        ),
      }}
    />
  );
};
