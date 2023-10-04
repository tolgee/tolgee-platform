import { styled } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { useState } from 'react';
import { useProject } from 'tg.hooks/useProject';
import { OrderTranslationDialog } from './OrderTranslationDialog';
import { TranslationProvider } from './TranslationProvider';
import { TRANSLATORS_LIST } from './translatorsList';
import { ProviderType } from './types';

const StyledContainer = styled('div')`
  display: grid;
  gap: 16px;
`;

export const OrderTranslation = () => {
  const { t } = useTranslate();

  const [activeProvider, setActiveProvider] = useState<ProviderType>();
  const project = useProject();

  return (
    <StyledContainer>
      <div>{t('project_order_translation_subtitle')}</div>

      {TRANSLATORS_LIST.map((provider) => (
        <TranslationProvider
          key={provider.id}
          provider={provider}
          onOrder={() => setActiveProvider(provider)}
        />
      ))}

      {activeProvider !== undefined && (
        <OrderTranslationDialog
          preselected={[project.id]}
          provider={activeProvider}
          onClose={() => setActiveProvider(undefined)}
        />
      )}
    </StyledContainer>
  );
};
