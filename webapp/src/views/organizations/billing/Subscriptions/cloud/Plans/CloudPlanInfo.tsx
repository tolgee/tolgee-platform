import { styled } from '@mui/material';
import { T } from '@tolgee/react';

import { useNumberFormatter } from 'tg.hooks/useLocale';
import { components } from 'tg.service/billingApiSchema.generated';
import { MtHint } from 'tg.component/billing/MtHint';
import { PlanInfo } from '../../common/PlanInfo';
import React from 'react';
import { StringsHint } from 'tg.component/billing/StringsHint';

type PlanModel = components['schemas']['CloudPlanModel'];

const StyledItem = styled('div')`
  display: grid;
  justify-items: center;
  color: ${({ theme }) => theme.palette.emphasis[700]};
`;

const StyledSpacer = styled('div')`
  width: 1px;
  background: ${({ theme }) => theme.palette.divider};
`;

const StyledNumber = styled('div')`
  font-size: 24px;
`;

const StyledName = styled('div')`
  font-size: 14px;
  text-align: center;
`;

type Props = {
  plan: PlanModel;
};

export const CloudPlanInfo: React.FC<Props> = ({ plan }) => {
  const usesSlots = plan.type === 'SLOTS_FIXED';
  const formatNumber = useNumberFormatter();
  return (
    <PlanInfo>
      <StyledItem>
        <StyledNumber>
          {formatNumber(
            usesSlots
              ? plan.includedUsage.translationSlots
              : plan.includedUsage.translations!
          )}
        </StyledNumber>
        <StyledName>
          {plan.type === 'PAY_AS_YOU_GO' ? (
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
          )}
        </StyledName>
      </StyledItem>
      <StyledSpacer />
      <StyledItem>
        <StyledNumber>
          {formatNumber(plan.includedUsage.mtCredits || 0)}
        </StyledNumber>
        <StyledName>
          <T
            keyName="billing_plan_credits_included"
            params={{ hint: <MtHint /> }}
          />
        </StyledName>
      </StyledItem>
    </PlanInfo>
  );
};
