import {
  satisfiesLanguageAccess,
  satisfiesPermission,
  ScopeWithLanguage,
  Scope,
} from 'tg.fixtures/permissions';
import { components } from 'tg.service/apiSchema.generated';

type PermissionModel = components['schemas']['PermissionModel'];
type PrivateUserAccountModel = components['schemas']['PrivateUserAccountModel'];

export const getPermissionTools = (
  permissions: PermissionModel,
  userInfo: PrivateUserAccountModel | undefined
) => {
  const scopes = permissions.scopes;
  const isAdmin = userInfo?.globalServerRole === 'ADMIN';

  return {
    satisfiesPermission(scope: Scope) {
      return satisfiesPermission(scopes, scope) || isAdmin;
    },
    satisfiesLanguageAccess(
      scope: ScopeWithLanguage,
      languageId: number | undefined
    ) {
      return satisfiesLanguageAccess(permissions, scope, languageId) || isAdmin;
    },
  };
};
