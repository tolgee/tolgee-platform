import { Translations } from './Translations';
import { TranslationsContextProvider } from './context/TranslationsContext';
import { useProject } from 'tg.hooks/useProject';
import { HeaderNsContext } from './context/HeaderNsContext';
import { ColumnsContext } from './context/ColumnsContext';

export const TranslationsView = () => {
  const project = useProject();

  return (
    <TranslationsContextProvider
      projectId={project.id}
      baseLang={project.baseLanguage?.tag}
      updateLocalStorageLanguages
    >
      <HeaderNsContext>
        <ColumnsContext>
          <Translations />
        </ColumnsContext>
      </HeaderNsContext>
    </TranslationsContextProvider>
  );
};
