import React, { FC, useRef, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { styled, keyframes, Tooltip, Box } from '@mui/material';
import { useSelector } from 'react-redux';
import clsx from 'clsx';

import { AppState } from 'tg.store/index';
import { BillingProgress } from 'tg.component/billing/BillingProgress';
import { LINKS, PARAMS } from 'tg.constants/links';
import {
  useConfig,
  usePreferredOrganization,
} from 'tg.hooks/InitialDataProvider';
import { UsageDetailed } from './UsageDetailed';
import { getProgressData } from './utils';
import { useBillingUsageData } from './useBillingUsageData';

export const USAGE_ELEMENT_ID = 'billing_organization_usage';

const StyledContainer = styled(Box)`
  display: flex;
  flex-direction: column;
  justify-content: center;
  font-size: 14px;
  color: ${({ theme }) => theme.palette.text.secondary};
  width: 100%;
  max-width: 200px;
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
  const config = useConfig();
  const planLimitErrors = useSelector(
    (state: AppState) => state.global.planLimitErrors
  );

  const previousShown = useRef(true);
  const firstRender = useRef(true);
  const [trigger, setTrigger] = useState(false);

  useEffect(() => {
    if (!firstRender.current && planLimitErrors) {
      setTrigger(true);
      usage.refetch();
      const timer = setTimeout(() => setTrigger(false), 1000);
      return () => {
        clearTimeout(timer);
        setTrigger(false);
      };
    }
    firstRender.current = false;
  }, [planLimitErrors]);

  const isOrganizationMember = Boolean(preferredOrganization.currentUserRole);
  const isOrganizationOwner = preferredOrganization.currentUserRole === 'OWNER';
  const statsEnabled = config.billing.enabled && isOrganizationMember;

  const usage = useBillingUsageData({
    organizationId: preferredOrganization.id,
    enabled: statsEnabled,
  });

  const progressData = usage.data && getProgressData(usage.data);

  const showStats =
    isOrganizationOwner ||
    planLimitErrors ||
    Number(progressData?.creditProgress) < 10 ||
    Number(progressData?.translationsProgress) < 10;

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
              <BillingProgress percent={progressData.translationsProgress} />
              <BillingProgress percent={progressData.creditProgress} />
            </StyledContent>
          </Tooltip>
        </OptionalLink>
      )}
    </StyledContainer>
  );
};
