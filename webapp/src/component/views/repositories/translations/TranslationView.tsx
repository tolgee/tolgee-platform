import * as React from 'react';
import { RepositoryLanguagesProvider } from '../../../../hooks/RepositoryLanguagesProvider';
import { TranslationsGrid } from '../../../Translations/TranslationsGrid';
import { TranslationGridContextProvider } from '../../../Translations/TtranslationsGridContextProvider';
import { LINKS } from '../../../../constants/links';
import { TranslationCreationDialog } from '../../../Translations/TranslationCreationDialog';
import { Route } from 'react-router-dom';

export const TranslationView = () => {
  return (
    <RepositoryLanguagesProvider>
      <TranslationGridContextProvider>
        <TranslationsGrid />
        <Route path={LINKS.REPOSITORY_TRANSLATIONS_ADD.template}>
          <TranslationCreationDialog />
        </Route>
      </TranslationGridContextProvider>
    </RepositoryLanguagesProvider>
  );
};
