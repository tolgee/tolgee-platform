import { useTranslate } from '@tolgee/react';
import { Tabs, Box, styled, Tab } from '@mui/material';
import { LINKS, PARAMS } from 'tg.constants/links';
import { useOrganization } from '../../../views/organizations/useOrganization';
import { BaseOrganizationSettingsView } from '../../../views/organizations/components/BaseOrganizationSettingsView';
import { useLlmProvidersViewItems } from './llmProvidersViewItems';
import { Link } from 'react-router-dom';
import { useState } from 'react';
import { LlmProviderCreateDialog } from './LlmProviderCreateDialog';

const StyledTabs = styled(Tabs)`
  margin-bottom: -1px;
`;

const StyledTabWrapper = styled(Box)`
  border-bottom: 1px solid ${({ theme }) => theme.palette.divider1};
`;

export const OrganizationLlmProvidersView = () => {
  const organization = useOrganization();
  const { t } = useTranslate();
  const [dialogOpen, setDialogOpen] = useState(false);

  const { ActiveComponent, items, value } = useLlmProvidersViewItems();

  return (
    <BaseOrganizationSettingsView
      windowTitle={t('organization_llm_providers_title')}
      title={t('organization_llm_providers_title')}
      link={LINKS.ORGANIZATION_LLM_PROVIDERS}
      addLabel={t('organization_llm_providers_add')}
      maxWidth="normal"
      onAdd={() => setDialogOpen(true)}
      navigation={[
        [
          t('organization_llm_providers_title'),
          LINKS.ORGANIZATION_LLM_PROVIDERS.build({
            [PARAMS.ORGANIZATION_SLUG]: organization!.slug,
          }),
        ],
      ]}
    >
      <StyledTabWrapper>
        <StyledTabs value={value}>
          {items.map((item) => {
            if (!item.tab.condition) {
              return null;
            }

            return (
              <Tab
                key={item.value}
                value={item.value}
                component={Link}
                to={item.link.build({
                  [PARAMS.ORGANIZATION_SLUG]: organization!.slug,
                })}
                label={item.tab.label}
                data-cy="organization-llm-providers-tab"
              />
            );
          })}
        </StyledTabs>
      </StyledTabWrapper>
      {ActiveComponent && <ActiveComponent />}
      {dialogOpen && (
        <LlmProviderCreateDialog onClose={() => setDialogOpen(false)} />
      )}
    </BaseOrganizationSettingsView>
  );
};
