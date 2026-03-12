import { Fragment } from 'react';
import { Box, styled } from '@mui/material';
import { useApiQuery, useBillingApiQuery } from 'tg.service/http/useQueryApi';
import { useOrganization } from 'tg.views/organizations/useOrganization';
import { useConfig } from 'tg.globalContext/helpers';
import { LlmProviderItem } from './LlmProviderItem';
import { LlmProviderPricingInfo } from './LlmProviderPricingInfo';
import { T } from '@tolgee/react';

const StyledContainer = styled(Box)<{ columns: number }>`
  display: grid;
  grid-template-columns: ${({ columns }) =>
    columns === 3
      ? 'minmax(35%, max-content) 1fr auto'
      : 'minmax(35%, max-content) 1fr'};
  align-items: center;
`;

export const LlmProvidersServer = () => {
  const organization = useOrganization();
  const config = useConfig();
  const billingEnabled = config.billing.enabled;

  const serverProvidersLoadable = useApiQuery({
    url: '/v2/organizations/{organizationId}/llm-providers/server-providers',
    method: 'get',
    path: {
      organizationId: organization!.id,
    },
  });

  const subscriptionLoadable = useBillingApiQuery({
    url: '/v2/organizations/{organizationId}/billing/subscription',
    method: 'get',
    path: {
      organizationId: organization!.id,
    },
    options: {
      enabled: billingEnabled,
      retry: false,
    },
  });

  const perThousandMtCredits =
    subscriptionLoadable.data?.plan?.prices?.perThousandMtCredits ?? null;

  return (
    <Box display="grid" gap={3} mt={3}>
      <Box>
        <T keyName="llm_providers_server_description" />
      </Box>
      <StyledContainer columns={billingEnabled ? 3 : 2}>
        {serverProvidersLoadable.data?._embedded?.providers?.map((p, i) => (
          <Fragment key={i}>
            <LlmProviderItem provider={p} />
            {billingEnabled && (
              <LlmProviderPricingInfo
                provider={p}
                perThousandMtCredits={perThousandMtCredits}
              />
            )}
          </Fragment>
        ))}
      </StyledContainer>
    </Box>
  );
};
