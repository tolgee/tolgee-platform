import { styled } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { useState } from 'react';
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
          provider={activeProvider}
          onClose={() => setActiveProvider(undefined)}
        />
      )}
    </StyledContainer>
  );
};
