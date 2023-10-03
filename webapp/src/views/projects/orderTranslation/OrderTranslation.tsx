import { styled } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { TranslationProvider } from './TranslationProvider';
import { TRANSLATORS_LIST } from './translatorsList';

const StyledContainer = styled('div')`
  display: grid;
  gap: 16px;
`;

export const OrderTranslation = () => {
  const { t } = useTranslate();
  return (
    <StyledContainer>
      <div>{t('project_order_translation_subtitle')}</div>

      {TRANSLATORS_LIST.map((provider) => (
        <TranslationProvider key={provider.id} provider={provider} />
      ))}
    </StyledContainer>
  );
};
