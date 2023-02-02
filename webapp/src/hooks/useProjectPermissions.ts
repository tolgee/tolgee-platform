import { useProject } from './useProject';
import { components } from 'tg.service/apiSchema.generated';

export type Scopes = components['schemas']['ComputedPermissionModel']['scopes'];
type ArrayElement<ArrayType extends readonly unknown[]> =
  ArrayType extends readonly (infer ElementType)[] ? ElementType : never;

export type Scope = ArrayElement<NonNullable<Scopes>>;

const SCOPE_TO_LANG_PROPERTY_MAP = {
  'translations.view': 'viewLanguageIds',
  'translations.edit': 'translateLanguageIds',
  'translations.state-edit': 'stateChangeLanguageIds',
};

export const useProjectPermissions = () => {
  const project = useProject();
  const scopes = project.computedPermission.scopes;

  function satisfiesPermission(scope: Scope): boolean {
    return !!scopes?.includes(scope);
  }

  function satisfiesLanguageAccess(
    scope: keyof typeof SCOPE_TO_LANG_PROPERTY_MAP,
    languageId: number | undefined
  ): boolean {
    if (!satisfiesPermission(scope)) {
      return false;
    }

    const allowedLanguages =
      project.computedPermission[SCOPE_TO_LANG_PROPERTY_MAP[scope]];

    if (!allowedLanguages?.length) {
      return true;
    }
    if (languageId !== undefined) {
      return Boolean(allowedLanguages?.includes(languageId));
    }
    return false;
  }

  return {
    satisfiesPermission,
    satisfiesLanguageAccess,
  };
};
