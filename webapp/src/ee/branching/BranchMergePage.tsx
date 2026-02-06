import { useProject } from 'tg.hooks/useProject';
import { TranslationsContextProvider } from 'tg.views/projects/translations/context/TranslationsContext';
import { useBranchFromUrlPath } from 'tg.component/branching/useBranchFromUrlPath';
import { BranchMergeDetail } from './merge/BranchMergeDetail';

export const BranchMergePage = () => {
  const project = useProject();
  const branchName = useBranchFromUrlPath();

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
