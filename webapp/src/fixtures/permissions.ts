import { components } from '../service/apiSchema.generated';

export type Scopes = components['schemas']['ComputedPermissionModel']['scopes'];
export type ProjectModel = components['schemas']['ProjectModel'];
type ArrayElement<ArrayType extends readonly unknown[]> =
  ArrayType extends readonly (infer ElementType)[] ? ElementType : never;

export type Scope = ArrayElement<NonNullable<Scopes>>;

export const SCOPE_TO_LANG_PROPERTY_MAP = {
  'translations.view': 'viewLanguageIds',
  'translations.edit': 'translateLanguageIds',
  'translations.state-edit': 'stateChangeLanguageIds',
};

export type ScopeWithLanguage = keyof typeof SCOPE_TO_LANG_PROPERTY_MAP;

export function satisfiesPermission(scopes: Scope[], scope: Scope): boolean {
  return !!scopes?.includes(scope);
}

export function satisfiesLanguageAccess(
  project: ProjectModel,
  scopes: Scope[],
  scope: keyof typeof SCOPE_TO_LANG_PROPERTY_MAP,
  languageId: number | undefined
): boolean {
  if (!satisfiesPermission(scopes, scope)) {
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
