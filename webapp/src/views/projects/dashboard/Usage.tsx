import React, { FC } from 'react';
import { Link } from 'react-router-dom';
import { styled, Tooltip, Typography, Box } from '@mui/material';
import { T } from '@tolgee/react';

import { useApiQuery } from 'tg.service/http/useQueryApi';
import { BillingProgress } from 'tg.component/billing/BillingProgress';
import { useGlobalLoading } from 'tg.component/GlobalLoading';
import { LINKS, PARAMS } from 'tg.constants/links';

const StyledContainer = styled('div')`
  display: grid;
  gap: 1px;
`;

const StyledTitle = styled('div')`
  display: grid;
  min-width: 250px;
  gap: 8px;
  padding: 8px;
`;

type UsageProps = {
  organizationId: number;
  slug: string;
};

export const Usage: FC<UsageProps> = (props) => {
  const usage = useApiQuery({
    url: '/v2/organizations/{organizationId}/usage',
    method: 'get',
    path: {
      organizationId: props.organizationId,
    },
    options: {
      refetchOnMount: false,
    },
  });

  const organization = useApiQuery({
    url: '/v2/organizations/{slug}',
    method: 'get',
    path: {
      slug: props.slug,
    },
    options: {
      refetchOnMount: false,
    },
  });

  useGlobalLoading(usage.isFetching || organization.isFetching);

  if (!usage.data || !organization.data) {
    return null;
  }

  const translationsAvailable =
    usage.data.translationLimit - usage.data.currentTranslations;
  const creditAvailable =
    usage.data.creditBalance + usage.data.extraCreditBalance;
  const translationsProgress =
    (translationsAvailable / usage.data.translationLimit) * 100;
  const creditLimit = usage.data.includedMtCredits;
  const creditsProgress = (creditAvailable / creditLimit) * 100;

  const isAdmin = organization.data.currentUserRole === 'OWNER';
  const displayTranslations = translationsProgress < 10 || isAdmin;
  const displayCredits = creditsProgress < 10 || isAdmin;

  const OptionalLink: React.FC = ({ children }) =>
    isAdmin ? (
      <Link
        to={LINKS.ORGANIZATION_BILLING.build({
          [PARAMS.ORGANIZATION_SLUG]: props.slug,
        })}
      >
        {children}
      </Link>
    ) : (
      <>{children}</>
    );

  return (
    <OptionalLink>
      <Tooltip
        title={
          <StyledTitle>
            <Box>
              <Typography variant="caption">
                <T
                  keyName="dashboard_billing_translations"
                  parameters={{
                    available: translationsAvailable,
                    max: usage.data.translationLimit,
                  }}
                />
              </Typography>
              <BillingProgress percent={translationsProgress} />
            </Box>
            <Box>
              <Typography variant="caption">
                <T
                  keyName="dashboard_billing_credit"
                  parameters={{
                    available: creditAvailable / 100,
                    max: creditLimit / 100,
                  }}
                />
              </Typography>
              <BillingProgress percent={creditsProgress} />
            </Box>
          </StyledTitle>
        }
      >
        <StyledContainer>
          {displayTranslations && (
            <BillingProgress percent={translationsProgress} />
          )}
          {displayCredits && <BillingProgress percent={creditsProgress} />}
        </StyledContainer>
      </Tooltip>
    </OptionalLink>
  );
};
