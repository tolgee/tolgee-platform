import {
  satisfiesLanguageAccess,
  satisfiesPermission,
  ScopeWithLanguage,
  SCOPE_TO_LANG_PROPERTY_MAP,
  Scope,
} from 'tg.fixtures/permissions';
import { components } from 'tg.service/apiSchema.generated';

type PermissionModel = components['schemas']['PermissionModel'];

export const getPermissionTools = (permissions: PermissionModel) => {
  const scopes = permissions.scopes;

  function allowedLanguages(scope: ScopeWithLanguage): boolean {
    if (!satisfiesPermission(scopes, scope)) {
      return false;
    }

    const allowedLanguages = permissions[SCOPE_TO_LANG_PROPERTY_MAP[scope]];

    return allowedLanguages || [];
  }

  return {
    scopes,
    satisfiesPermission(scope: Scope) {
      return satisfiesPermission(scopes, scope);
    },
    satisfiesLanguageAccess(
      scope: ScopeWithLanguage,
      languageId: number | undefined
    ) {
      return satisfiesLanguageAccess(permissions, scope, languageId);
    },
    allowedLanguages,
  };
};
