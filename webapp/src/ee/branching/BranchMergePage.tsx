import { useProject } from 'tg.hooks/useProject';
import { TranslationsContextProvider } from 'tg.views/projects/translations/context/TranslationsContext';
import { BranchMergeDetail } from 'tg.ee.module/branching/merge/BranchMergeDetail';

export const BranchMergePage = () => {
  const project = useProject();

  return (
    <TranslationsContextProvider
      projectId={project.id}
      baseLang={project.baseLanguage?.tag}
      aiPlayground={false}
    >
      <BranchMergeDetail />
    </TranslationsContextProvider>
  );
};
