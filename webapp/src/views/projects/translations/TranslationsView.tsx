import { Translations } from './Translations';
import { TranslationsContextProvider } from './context/TranslationsContext';
import { useProject } from 'tg.hooks/useProject';
import { HeaderNsContext } from './context/HeaderNsContext';
import { usePrefilter } from './prefilters/usePrefilter';
import { useUrlSearchState } from 'tg.hooks/useUrlSearchState';
import { QUERY } from 'tg.constants/links';
import { useBranchFromUrl } from 'tg.component/branching/useBranchFromUrl';

export const TranslationsView = () => {
  const project = useProject();
  const branchName = useBranchFromUrl();

  const prefilter = usePrefilter();
  const [aiPlayground] = useUrlSearchState(QUERY.TRANSLATIONS_AI_PLAYGROUND, {
    defaultVal: undefined,
    history: false,
  });
  return (
    <TranslationsContextProvider
      projectId={project.id}
      branchName={branchName}
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
