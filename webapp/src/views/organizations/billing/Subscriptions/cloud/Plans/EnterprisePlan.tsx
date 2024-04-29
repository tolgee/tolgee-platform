import { T } from '@tolgee/react';
import { Button } from '@mui/material';

import { MtHint } from 'tg.component/billing/MtHint';
import { StringsHint } from 'tg.component/billing/StringsHint';
import { StyledActionArea } from 'tg.views/organizations/billing/BillingSection';

import { Plan, PlanContent } from '../../common/Plan';
import { PlanInfoArea } from '../../common/PlanInfo';
import { PlanTitle } from '../../common/PlanTitle';
import { IncludedFeatures } from '../../selfHostedEe/IncludedFeatures';
import { PlanMetrics } from '../../common/PlanMetrics';

export const EnterprisePlan = () => {
  return (
    <Plan>
      <PlanContent>
        <PlanTitle title="Enterprise" />

        <PlanInfoArea>
          <PlanMetrics
            left={{
              number: <T keyName="billing_plan_unlimited" />,
              name: (
                <T
                  keyName="billing_plan_strings_included_with_hint"
                  params={{ hint: <StringsHint /> }}
                />
              ),
            }}
            right={{
              number: <T keyName="billing_plan_unlimited" />,
              name: (
                <T
                  keyName="billing_plan_credits_included"
                  params={{ hint: <MtHint /> }}
                />
              ),
            }}
          />
          <IncludedFeatures
            features={[
              'ACCOUNT_MANAGER',
              'PREMIUM_SUPPORT',
              'DEDICATED_SLACK_CHANNEL',
              'TEAM_TRAINING',
            ]}
          />
        </PlanInfoArea>
        <StyledActionArea>
          <Button
            variant="outlined"
            color="primary"
            size="small"
            href="mailto:info@tolgee.io"
          >
            <T keyName="billing_plan_contact_us" />
          </Button>
        </StyledActionArea>
      </PlanContent>
    </Plan>
  );
};
