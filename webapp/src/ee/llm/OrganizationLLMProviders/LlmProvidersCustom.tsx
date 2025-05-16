import { Box, styled } from '@mui/material';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { useOrganization } from 'tg.views/organizations/useOrganization';
import { LlmProviderItem } from './LlmProviderItem';
import { useState } from 'react';
import { LlmProviderModel } from 'tg.translationTools/useLlmProviderTranslation';
import { LlmProviderEditDialog } from './LlmProviderEditDialog';
import { EmptyListMessage } from 'tg.component/common/EmptyListMessage';
import { useTranslate } from '@tolgee/react';

const StyledContainer = styled(Box)`
  display: grid;
  margin-top: 16px;
  grid-template-columns: minmax(35%, max-content) 1fr auto;
  align-items: center;
`;

export const LlmProvidersCustom = () => {
  const { t } = useTranslate();
  const [editItem, setEditItem] = useState<LlmProviderModel>();
  const organization = useOrganization();
  const providersLoadable = useApiQuery({
    url: '/v2/organizations/{organizationId}/llm-providers',
    method: 'get',
    path: {
      organizationId: organization!.id,
    },
  });

  if (
    !providersLoadable.data?._embedded?.providers?.length &&
    providersLoadable.isSuccess
  ) {
    return (
      <EmptyListMessage>
        {t('llm_providers_custom_empty_message')}
      </EmptyListMessage>
    );
  }

  return (
    <>
      <StyledContainer>
        {providersLoadable.data?._embedded?.providers?.map((p) => (
          <LlmProviderItem
            key={p.id}
            provider={p}
            onEdit={() => setEditItem(p)}
          />
        ))}
      </StyledContainer>
      {editItem && (
        <LlmProviderEditDialog
          provider={editItem}
          onClose={() => setEditItem(undefined)}
        />
      )}
    </>
  );
};
