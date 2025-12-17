import { FC } from 'react';
import { TranslationList, StyledLanguageField } from './KeyPanelBase';
import { CellTranslation } from 'tg.views/projects/translations/TranslationsList/CellTranslation';
import { useTranslationsSelector } from 'tg.views/projects/translations/context/TranslationsContext';

export const KeyTranslations: FC<{
  keyData: any;
}> = ({ keyData }) => {
  const languages = useTranslationsSelector((c) => c.languages);
  return (
    <TranslationList>
      {Object.entries(keyData.translations ?? {}).map(([lang]) => {
        const language = languages?.find((l) => l.tag === lang);
        if (!language) return null;
        return (
          <StyledLanguageField
            key={lang}
            data-cy="translation-edit-translation-field"
          >
            <CellTranslation
              data={keyData}
              language={language}
              active={false}
              lastFocusable={false}
              readonly={true}
            />
          </StyledLanguageField>
        );
      })}
    </TranslationList>
  );
};
