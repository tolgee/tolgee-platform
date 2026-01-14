import { useProject } from 'tg.hooks/useProject';
import { TranslationsContextProvider } from 'tg.views/projects/translations/context/TranslationsContext';
import { BranchMergeDetail } from 'tg.ee.module/branching/merge/BranchMergeDetail';
import { useBranchFromUrl } from 'tg.component/branching/useBranchFromUrl';

export const BranchMergePage = () => {
  const project = useProject();
  const branchName = useBranchFromUrl();

  return (
    <TranslationsContextProvider
      projectId={project.id}
      branchName={branchName}
      baseLang={project.baseLanguage?.tag}
      aiPlayground={false}
    >
      <BranchMergeDetail />
    </TranslationsContextProvider>
  );
};
