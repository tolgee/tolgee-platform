import { Translations } from './Translations';
import { TranslationsContextProvider } from './context/TranslationsContext';
import { useProject } from 'tg.hooks/useProject';
import { HeaderNsContext } from './context/HeaderNsContext';
import { usePrefilter } from './prefilters/usePrefilter';
import { useUrlSearchState } from 'tg.hooks/useUrlSearchState';

export const TranslationsView = () => {
  const project = useProject();

  const prefilter = usePrefilter();
  const [aiPlayground] = useUrlSearchState('aiPlayground', {
    defaultVal: undefined,
    history: false,
  });
  return (
    <TranslationsContextProvider
      projectId={project.id}
      baseLang={project.baseLanguage?.tag}
      updateLocalStorageLanguages
      prefilter={prefilter}
      aiPlayground={Boolean(aiPlayground)}
    >
      <HeaderNsContext>
        <Translations />
      </HeaderNsContext>
    </TranslationsContextProvider>
  );
};
