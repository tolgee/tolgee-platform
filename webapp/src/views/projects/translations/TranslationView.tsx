import { ProjectLanguagesProvider } from 'tg.hooks/ProjectLanguagesProvider';
import { TranslationsGrid } from './TranslationsGrid';
import { TranslationGridContextProvider } from './TtranslationsGridContextProvider';
import { LINKS } from 'tg.constants/links';
import { TranslationCreationDialog } from './TranslationCreationDialog';
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
