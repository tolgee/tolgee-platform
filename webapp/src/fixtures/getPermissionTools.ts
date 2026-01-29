import {
  satisfiesLanguageAccess,
  satisfiesPermission,
  satisfiesPermissionWithBranching,
  ScopeWithLanguage,
  Scope,
} from 'tg.fixtures/permissions';
import { components } from 'tg.service/apiSchema.generated';
import { useBranchEditAccess } from 'tg.views/projects/translations/context/services/useBranchEditAccess';

type PermissionModel = components['schemas']['PermissionModel'];

export const getPermissionTools = (permissions: PermissionModel) => {
  const scopes = permissions.scopes;

  return {
    satisfiesPermission(scope: Scope) {
      return satisfiesPermission(scopes, scope);
    },
    satisfiesLanguageAccess(
      scope: ScopeWithLanguage,
      languageId: number | undefined
    ) {
      return satisfiesLanguageAccess(permissions, scope, languageId);
    },
    satisfiesPermissionWithBranching(scope: Scope) {
      return satisfiesPermissionWithBranching(
        scopes,
        scope,
        useBranchEditAccess()
      );
    },
  };
};
