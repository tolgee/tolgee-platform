import {
  satisfiesPermissionWithBranching,
  Scope,
} from 'tg.fixtures/permissions';
import { useBranchEditAccess } from 'tg.views/projects/translations/context/services/useBranchEditAccess';
import { useProject } from 'tg.hooks/useProject';

export const useSatisfiesPermissionWithBranching = () => {
  const project = useProject();
  const canEditProtectedBranch = useBranchEditAccess();

  return (scope: Scope) =>
    satisfiesPermissionWithBranching(
      project.computedPermission.scopes,
      scope,
      canEditProtectedBranch
    );
};
