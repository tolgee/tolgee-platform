import { useProject } from './useProject';
import { components } from 'tg.service/apiSchema.generated';

export type Scopes = components['schemas']['ComputedPermissionModel']['scopes'];
type ArrayElement<ArrayType extends readonly unknown[]> =
  ArrayType extends readonly (infer ElementType)[] ? ElementType : never;

export type Scope = ArrayElement<NonNullable<Scopes>>;

export const useProjectPermissions = () => {
  const project = useProject();
  const scopes = project.computedPermission.scopes;
  const permittedLanguages = project.computedPermission.permittedLanguageIds;

  function satisfiesPermission(scope: Scope): boolean {
    return !!scopes?.includes(scope);
  }

  function canEditLanguage(language: number | undefined): boolean {
    if (satisfiesPermission('translations.edit')) {
      return false;
    }
    if (!permittedLanguages?.length) {
      return true;
    }
    if (language !== undefined) {
      return !!permittedLanguages?.includes(language);
    }
    return false;
  }

  return {
    satisfiesPermission,
    canEditLanguage,
  };
};
