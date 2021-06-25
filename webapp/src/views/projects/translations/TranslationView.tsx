import { Route } from 'react-router-dom';

import { LINKS } from 'tg.constants/links';
import { ProjectLanguagesProvider } from 'tg.hooks/ProjectLanguagesProvider';

import { TranslationCreationDialog } from './TranslationCreationDialog';
import { TranslationsGrid } from './TranslationsGrid';
import { TranslationGridContextProvider } from './TtranslationsGridContextProvider';

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
