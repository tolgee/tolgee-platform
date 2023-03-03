import { useProject } from './useProject';
import {
  satisfiesLanguageAccess,
  satisfiesPermission,
  ScopeWithLanguage,
  SCOPE_TO_LANG_PROPERTY_MAP,
  Scope,
} from 'tg.fixtures/permissions';

export const useProjectPermissions = () => {
  const project = useProject();
  const scopes = project.computedPermission.scopes;

  function allowedLanguages(scope: ScopeWithLanguage): boolean {
    if (!satisfiesPermission(scopes, scope)) {
      return false;
    }

    const allowedLanguages =
      project.computedPermission[SCOPE_TO_LANG_PROPERTY_MAP[scope]];

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
      return satisfiesLanguageAccess(project, scopes, scope, languageId);
    },
    allowedLanguages,
  };
};
