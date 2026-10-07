import {
  satisfiesLanguageAccess,
  satisfiesPermission,
  ScopeWithLanguage,
  Scope,
} from 'tg.fixtures/permissions';
import { components } from 'tg.service/apiSchema.generated';

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
  };
};
