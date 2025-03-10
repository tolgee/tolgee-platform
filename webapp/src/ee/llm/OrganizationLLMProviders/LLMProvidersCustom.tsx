import { Box, styled } from '@mui/material';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { useOrganization } from 'tg.views/organizations/useOrganization';
import { LLMProviderItem } from './LLMProviderItem';
import { useState } from 'react';
import { LLMProviderModel } from 'tg.translationTools/useLLMProviderTranslation';
import { LLMProviderEditDialog } from './LLMProviderEditDialog';

const StyledContainer = styled(Box)`
  display: grid;
  margin-top: 16px;
  grid-template-columns: minmax(35%, max-content) 1fr auto;
  align-items: center;
`;

export const LLMProvidersCustom = () => {
  const [editItem, setEditItem] = useState<LLMProviderModel>();
  const organization = useOrganization();
  const providersLoadable = useApiQuery({
    url: '/v2/organizations/{organizationId}/llm-providers',
    method: 'get',
    path: {
      organizationId: organization!.id,
    },
  });

  return (
    <>
      <StyledContainer>
        {providersLoadable.data?.items.map((p) => (
          <LLMProviderItem
            key={p.id}
            provider={p}
            onEdit={() => setEditItem(p)}
          />
        ))}
      </StyledContainer>
      {editItem && (
        <LLMProviderEditDialog
          provider={editItem}
          onClose={() => setEditItem(undefined)}
        />
      )}
    </>
  );
};
