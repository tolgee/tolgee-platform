import clsx from 'clsx';
import React, { FC, useRef, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { styled, keyframes, Tooltip, Box } from '@mui/material';

import { CircularBillingProgress } from './CircularBillingProgress';
import { LINKS, PARAMS } from 'tg.constants/links';
import {
  useOrganizationUsage,
  usePreferredOrganization,
} from 'tg.globalContext/helpers';
import { UsageDetailed } from './UsageDetailed';
import { getProgressData } from './utils';
import { BILLING_CRITICAL_PERCENT } from './constants';

export const USAGE_ELEMENT_ID = 'billing_organization_usage';

const StyledContainer = styled(Box)`
  display: flex;
  flex-direction: column;
  justify-content: center;
  font-size: 14px;
  color: ${({ theme }) => theme.palette.text.secondary};
  margin-bottom: -4px;
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
  background: ${({ theme }) => theme.palette.background.default};
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

export const Usage: FC = () => {
  const { preferredOrganization } = usePreferredOrganization();
  const { planLimitErrors, noCreditErrors } = useOrganizationUsage();

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
  }, [planLimitErrors, noCreditErrors]);

  const isOrganizationOwner = preferredOrganization.currentUserRole === 'OWNER';

  const { usage } = useOrganizationUsage();

  const progressData = usage && getProgressData(usage);

  const showStats =
    planLimitErrors ||
    noCreditErrors ||
    Number(progressData?.creditProgress) < BILLING_CRITICAL_PERCENT ||
    Number(progressData?.translationsProgress) < BILLING_CRITICAL_PERCENT;

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

  return (
    <StyledContainer
      sx={{ zIndex: trigger ? 999999 : undefined }}
      id={USAGE_ELEMENT_ID}
    >
      {progressData && showStats && (
        <OptionalLink>
          <Tooltip
            title={
              <StyledTitle>
                <UsageDetailed {...progressData} />
              </StyledTitle>
            }
          >
            <StyledContent className={clsx({ triggered: Boolean(trigger) })}>
              <CircularBillingProgress
                percent={Math.min(
                  progressData.translationsProgress,
                  progressData.creditProgress
                )}
              />
            </StyledContent>
          </Tooltip>
        </OptionalLink>
      )}
    </StyledContainer>
  );
};
