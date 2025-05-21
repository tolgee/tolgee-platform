import { Box, styled } from '@mui/material';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { useOrganization } from 'tg.views/organizations/useOrganization';
import { LlmProviderItem } from './LlmProviderItem';
import { T } from '@tolgee/react';

const StyledContainer = styled(Box)`
  display: grid;
  grid-template-columns: minmax(35%, max-content) 1fr;
  align-items: center;
`;

export const LlmProvidersServer = () => {
  const organization = useOrganization();

  const serverProvidersLoadable = useApiQuery({
    url: '/v2/organizations/{organizationId}/llm-providers/server-providers',
    method: 'get',
    path: {
      organizationId: organization!.id,
    },
  });

  return (
    <Box display="grid" gap={3} mt={3}>
      <Box>
        <T keyName="llm_providers_server_description" />
      </Box>
      <StyledContainer>
        {serverProvidersLoadable.data?._embedded?.providers?.map((p, i) => (
          <LlmProviderItem key={i} provider={p} />
        ))}
      </StyledContainer>
    </Box>
  );
};
