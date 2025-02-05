import { useTranslate } from '@tolgee/react';
import { PlanSubtitle } from './PlanStyles';
import { Box, styled } from '@mui/material';

const StyledTopContainer = styled(Box)`
  position: absolute;
  display: flex;
  justify-content: center;
  top: 0px;
  left: 0px;
  right: 0px;
  padding-top: 8px;
`;

const StyledCustomChip = styled(Box)`
  font-size: 14px;
  font-weight: 400;
  line-height: 18px;
  max-height: 24px;
  padding: 3px 6px;
  border-radius: 12px;
  color: ${({ theme }) => theme.palette.tokens.info.main};
  background: ${({ theme }) => theme.palette.tokens.info._states.hover};
  justify-self: center;
  padding: 3px 6px;
  &.noBackground {
    background: transparent;
  }
`;

const StyledCustomLabel = styled('span')`
  font-size: 14px;
  font-weight: 400;
  text-transform: lowercase;
`;

type Props = {
  active: boolean;
  ended: boolean;
  custom?: boolean;
  activeTrial?: boolean;
};

export const PlanActiveBanner = ({
  active,
  ended,
  custom,
  activeTrial,
}: Props) => {
  const { t } = useTranslate();

  if (active) {
    return (
      <PlanSubtitle data-cy="billing-plan-subtitle">
        <span>
          {ended
            ? t('billing_subscription_cancelled')
            : activeTrial
            ? t('billing_subscription_active_trial')
            : t('billing_subscription_active')}
        </span>
        {custom && (
          <>
            {' '}
            <StyledCustomLabel className="noBackground">
              {t('billing_subscription_custom')}
            </StyledCustomLabel>
          </>
        )}
      </PlanSubtitle>
    );
  } else if (custom) {
    return (
      <StyledTopContainer>
        <StyledCustomChip>{t('billing_subscription_custom')}</StyledCustomChip>
      </StyledTopContainer>
    );
  } else {
    return null;
  }
};
