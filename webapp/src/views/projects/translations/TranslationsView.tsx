import { Translations } from './Translations';
import { TranslationsContextProvider } from './context/TranslationsContext';
import { useProject } from 'tg.hooks/useProject';
import { HeaderNsContext } from './context/HeaderNsContext';
import { useUrlSearch } from 'tg.hooks/useUrlSearch';

export const TranslationsView = () => {
  const project = useProject();
  const revisionFilter = Number(useUrlSearch().revision);

  return (
    <TranslationsContextProvider
      projectId={project.id}
      baseLang={project.baseLanguage?.tag}
      updateLocalStorageLanguages
      revisionFilter={revisionFilter}
    >
      <HeaderNsContext>
        <Translations />
      </HeaderNsContext>
    </TranslationsContextProvider>
  );
};
