import { useProject } from './useProject';
import { components } from 'tg.service/apiSchema.generated';

export type Scopes = components['schemas']['ComputedPermissionModel']['scopes'];
type ArrayElement<ArrayType extends readonly unknown[]> =
  ArrayType extends readonly (infer ElementType)[] ? ElementType : never;

export type Scope = ArrayElement<NonNullable<Scopes>>;

export class ProjectPermissions {
  constructor(
    public scopes: Scopes,
    public permittedLanguages: number[] | undefined
  ) {}

  canEditLanguage(language: number | undefined): boolean {
    if (!this.satisfiesPermission('translations.edit')) {
      return false;
    }

    if (!this.permittedLanguages?.length) {
      return true;
    }

    if (language !== undefined) {
      return !!this.permittedLanguages?.includes(language);
    }

    return false;
  }

  satisfiesPermission(scope: Scope): boolean {
    return !!this.scopes?.includes(scope);
  }
}

export const useProjectPermissions = (): ProjectPermissions => {
  const project = useProject();
  const scopes = project.computedPermission.scopes;
  return new ProjectPermissions(
    scopes,
    project.computedPermission.permittedLanguageIds
  );
};
