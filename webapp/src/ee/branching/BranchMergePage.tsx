import { useProject } from 'tg.hooks/useProject';
import { TranslationsContextProvider } from 'tg.views/projects/translations/context/TranslationsContext';
import { BranchMergeDetailView } from 'tg.ee.module/branching/BranchMergeDetailView';

export const BranchMergePage = () => {
  const project = useProject();

  return (
    <TranslationsContextProvider
      projectId={project.id}
      baseLang={project.baseLanguage?.tag}
      aiPlayground={false}
    >
      <BranchMergeDetailView />
    </TranslationsContextProvider>
  );
};
