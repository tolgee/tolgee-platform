import { Box, styled } from '@mui/material';
import { T } from '@tolgee/react';

import { useNumberFormatter } from 'tg.hooks/useLocale';
import { components } from 'tg.service/billingApiSchema.generated';
import { MtHint } from 'tg.component/billing/MtHint';

type PlanModel = components['schemas']['PlanModel'];

const StyledInfo = styled(Box)`
  display: grid;
  justify-content: space-between;
  grid-template-columns: 1fr 16px 1fr;
  padding-bottom: 8px;
  justify-items: center;
`;

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

export const PlanInfo: React.FC<Props> = ({ plan }) => {
  const formatNumber = useNumberFormatter();
  return (
    <StyledInfo gridArea="info">
      <StyledItem>
        <StyledNumber>{formatNumber(plan.translationLimit!)}</StyledNumber>
        <StyledName>
          <T keyName="billing_plan_translation_limit" />
        </StyledName>
      </StyledItem>
      <StyledSpacer />
      <StyledItem>
        <StyledNumber>
          {formatNumber((plan.includedMtCredits || 0) / 100)}
        </StyledNumber>
        <StyledName>
          <T
            keyName="billing_plan_credits_included"
            params={{ hint: <MtHint /> }}
          />
        </StyledName>
      </StyledItem>
    </StyledInfo>
  );
};
