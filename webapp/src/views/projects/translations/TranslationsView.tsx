import { Translations } from './Translations';
import { TranslationsContextProvider } from './context/TranslationsContext';
import { useProject } from 'tg.hooks/useProject';

export const TranslationsView = () => {
  const project = useProject();

  return (
    <TranslationsContextProvider
      projectId={project.id}
      updateLocalStorageLanguages
    >
      <Translations />
    </TranslationsContextProvider>
  );
};
