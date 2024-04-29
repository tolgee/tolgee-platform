import { T } from '@tolgee/react';
import { Button } from '@mui/material';
import { StyledActionArea } from 'tg.views/organizations/billing/BillingSection';

import { Plan, PlanContent } from '../common/Plan';
import { PlanInfoArea } from '../common/PlanInfo';
import { PlanTitle } from '../common/PlanTitle';
import { IncludedFeatures } from '../selfHostedEe/IncludedFeatures';

export const EnterprisePlan = () => {
  return (
    <Plan>
      <PlanContent>
        <PlanTitle title="Enterprise" />

        <PlanInfoArea>
          <IncludedFeatures
            includedUsage={{
              seats: -1,
            }}
            features={[
              'ACCOUNT_MANAGER',
              'PREMIUM_SUPPORT',
              'DEDICATED_SLACK_CHANNEL',
              'DEPLOYMENT_ASSISTANCE',
              'ASSISTED_UPDATES',
              'BACKUP_CONFIGURATION',
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
