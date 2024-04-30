import { Translations } from './Translations';
import { TranslationsContextProvider } from './context/TranslationsContext';
import { useProject } from 'tg.hooks/useProject';
import { HeaderNsContext } from './context/HeaderNsContext';
import { useActivityFilter } from './useActivityFilter';

export const TranslationsView = () => {
  const project = useProject();

  const { revisionId } = useActivityFilter();

  return (
    <TranslationsContextProvider
      projectId={project.id}
      baseLang={project.baseLanguage?.tag}
      updateLocalStorageLanguages
      revisionFilter={revisionId}
    >
      <HeaderNsContext>
        <Translations revisionId={revisionId} />
      </HeaderNsContext>
    </TranslationsContextProvider>
  );
};
