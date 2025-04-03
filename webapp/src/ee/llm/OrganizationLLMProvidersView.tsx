import { useTranslate } from '@tolgee/react';
import { BaseOrganizationSettingsView } from '../../views/organizations/components/BaseOrganizationSettingsView';
import { useOrganization } from '../../views/organizations/useOrganization';
import { LINKS, PARAMS } from 'tg.constants/links';
import { Box, IconButton, Typography } from '@mui/material';
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import { useState } from 'react';
import { LLMProviderCreateDialog } from '../../views/organizations/llmProviders/LLMProviderCreateDialog';
import { Trash01 } from '@untitled-ui/icons-react';

export const OrganizationLLMProvidersView = () => {
  const organization = useOrganization();
  const { t } = useTranslate();
  const [dialogOpen, setDialogOpen] = useState(false);

  const providersLoadable = useApiQuery({
    url: '/v2/organizations/{organizationId}/llm-providers',
    method: 'get',
    path: {
      organizationId: organization!.id,
    },
  });

  const serverProvidersLoadable = useApiQuery({
    url: '/v2/organizations/{organizationId}/llm-providers/server-providers',
    method: 'get',
    path: {
      organizationId: organization!.id,
    },
  });

  const deleteLoadable = useApiMutation({
    url: '/v2/organizations/{organizationId}/llm-providers/{providerId}',
    method: 'delete',
    invalidatePrefix: '/v2/organizations/{organizationId}/llm-providers',
  });

  return (
    <BaseOrganizationSettingsView
      windowTitle={t('organization_llm_providers_title')}
      title={t('organization_llm_providers_title')}
      link={LINKS.ORGANIZATION_LLM_PROVIDERS}
      addLabel={t('organization_llm_providers_add')}
      onAdd={() => {
        setDialogOpen(true);
      }}
      maxWidth="normal"
      navigation={[
        [
          t('organization_llm_providers_title'),
          LINKS.ORGANIZATION_LLM_PROVIDERS.build({
            [PARAMS.ORGANIZATION_SLUG]: organization!.slug,
          }),
        ],
      ]}
    >
      <Typography variant="h3">Custom</Typography>
      <Box display="grid" mb={2}>
        {providersLoadable.data?.items.map((p) => {
          return (
            <Box
              display="flex"
              justifyContent="space-between"
              key={p.id}
              sx={{ borderBottom: '1px solid gray' }}
            >
              <Box>
                {p.name}, {p.type}
              </Box>
              <IconButton
                size="small"
                onClick={() => {
                  deleteLoadable.mutate({
                    path: {
                      organizationId: organization!.id,
                      providerId: p.id,
                    },
                  });
                }}
              >
                <Trash01 width={18} height={18} />
              </IconButton>
            </Box>
          );
        })}
      </Box>
      <Typography variant="h3">Server</Typography>
      <Box display="grid" mb={2}>
        {serverProvidersLoadable.data?.items.map((p, i) => {
          return (
            <Box key={i}>
              {p.name}, {p.type}
            </Box>
          );
        })}
      </Box>
      {dialogOpen && (
        <LLMProviderCreateDialog onClose={() => setDialogOpen(false)} />
      )}
    </BaseOrganizationSettingsView>
  );
};
