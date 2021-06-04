import * as React from 'react';
import { ProjectLanguagesProvider } from '../../../../hooks/ProjectLanguagesProvider';
import { TranslationsGrid } from '../../../Translations/TranslationsGrid';
import { TranslationGridContextProvider } from '../../../Translations/TtranslationsGridContextProvider';
import { LINKS } from '../../../../constants/links';
import { TranslationCreationDialog } from '../../../Translations/TranslationCreationDialog';
import { Route } from 'react-router-dom';

export const TranslationView = () => {
  return (
    <ProjectLanguagesProvider>
      <TranslationGridContextProvider>
        <TranslationsGrid />
        <Route path={LINKS.PROJECT_TRANSLATIONS_ADD.template}>
          <TranslationCreationDialog />
        </Route>
      </TranslationGridContextProvider>
    </ProjectLanguagesProvider>
  );
};
