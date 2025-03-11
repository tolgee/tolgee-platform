import clsx from 'clsx';
import React, { FC, useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { Box, keyframes, styled, Tooltip } from '@mui/material';

import { CircularBillingProgress } from './CircularBillingProgress';
import { LINKS, PARAMS } from 'tg.constants/links';
import {
  useOrganizationUsage,
  usePreferredOrganization,
} from 'tg.globalContext/helpers';
import { UsageDetailed } from './UsageDetailed';
import { getProgressData } from './getProgressData';

export const USAGE_ELEMENT_ID = 'billing_organization_usage';

const StyledContainer = styled(Box)`
  display: flex;
  flex-direction: column;
  justify-content: center;
  font-size: 14px;
  color: ${({ theme }) => theme.palette.text.secondary};
`;

const shakeAnimation = keyframes`
  10%, 90% {
    transform: translate3d(-1px, 0, 0);
  }
  
  20%, 80% {
    transform: translate3d(2px, 0, 0);
  }

  30%, 50%, 70% {
    transform: translate3d(-4px, 0, 0);
  }

  40%, 60% {
    transform: translate3d(4px, 0, 0);
  }
`;

const StyledContent = styled('div')`
  display: grid;
  gap: 1px;
  &.triggered {
    animation: ${shakeAnimation} 700ms linear;
  }
`;

const StyledTitle = styled('div')`
  display: grid;
  min-width: 250px;
  gap: 8px;
  padding: 8px;
`;

export const CriticalUsageCircle: FC = () => {
  const { preferredOrganization } = usePreferredOrganization();
  const { planLimitErrors } = useOrganizationUsage();

  const previousShown = useRef(true);
  const firstRender = useRef(true);
  const [trigger, setTrigger] = useState(false);

  useEffect(() => {
    if (!firstRender.current && planLimitErrors) {
      setTrigger(true);
      const timer = setTimeout(() => setTrigger(false), 1000);
      return () => {
        clearTimeout(timer);
        setTrigger(false);
      };
    }
    firstRender.current = false;
  }, [planLimitErrors]);

  const isOrganizationOwner =
    preferredOrganization?.currentUserRole === 'OWNER';

  const { usage } = useOrganizationUsage();

  const progressData = usage && getProgressData({ usage });

  const showStats = planLimitErrors || progressData?.isCritical;

  previousShown.current = Boolean(showStats);

  const OptionalLink: React.FC = ({ children }) =>
    isOrganizationOwner ? (
      <Link
        to={LINKS.ORGANIZATION_BILLING.build({
          [PARAMS.ORGANIZATION_SLUG]: preferredOrganization.slug,
        })}
      >
        {children}
      </Link>
    ) : (
      <>{children}</>
    );

  if (!progressData || !showStats) {
    return null;
  }

  return (
    <StyledContainer
      sx={{ zIndex: trigger ? 999999 : undefined }}
      id={USAGE_ELEMENT_ID}
    >
      <OptionalLink>
        <Tooltip
          title={
            <StyledTitle>
              <UsageDetailed
                {...progressData}
                isPayAsYouGo={usage?.isPayAsYouGo}
              />
            </StyledTitle>
          }
        >
          <StyledContent className={clsx({ triggered: Boolean(trigger) })}>
            <CircularBillingProgress
              isPayAsYouGo={usage.isPayAsYouGo}
              value={progressData.mostCriticalProgress}
              maxValue={1}
            />
          </StyledContent>
        </Tooltip>
      </OptionalLink>
    </StyledContainer>
  );
};
