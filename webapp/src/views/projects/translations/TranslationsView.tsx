import { Translations } from './Translations';
import { TranslationsContextProvider } from './context/TranslationsContext';
import { useProject } from 'tg.hooks/useProject';
import { HeaderNsContext } from './context/HeaderNsContext';

export const TranslationsView = () => {
  const project = useProject();

  return (
    <TranslationsContextProvider
      projectId={project.id}
      baseLang={project.baseLanguage?.tag}
      updateLocalStorageLanguages
    >
      <HeaderNsContext>
        <Translations />
      </HeaderNsContext>
    </TranslationsContextProvider>
  );
};
