import { ProjectPermissionType } from '../service/response.types';
import { useProject } from './useProject';
import { useIsAdmin } from 'tg.globalContext/helpers';

export class ProjectPermissions {
  private static readonly ORDERED_PERMISSIONS = [
    ProjectPermissionType.VIEW,
    ProjectPermissionType.TRANSLATE,
    ProjectPermissionType.EDIT,
    ProjectPermissionType.MANAGE,
  ];

  constructor(
    private activePermission: ProjectPermissionType,
    private permittedLanguages: number[] | undefined,
    private isAdmin: boolean
  ) {}

  canEditLanguage(language: number | undefined) {
    if (this.satisfiesPermission(ProjectPermissionType.EDIT)) {
      return true;
    } else if (
      language !== undefined &&
      this.activePermission === ProjectPermissionType.TRANSLATE
    ) {
      return (
        !this.permittedLanguages?.length ||
        Boolean(this.permittedLanguages?.includes(language))
      );
    }
    return false;
  }

  satisfiesPermission(type: ProjectPermissionType) {
    if (this.isAdmin) {
      return true;
    }
    const requiredPower = ProjectPermissions.ORDERED_PERMISSIONS.findIndex(
      (p) => p === type
    );
    const activePower = ProjectPermissions.ORDERED_PERMISSIONS.findIndex(
      (p) => p === this.activePermission
    );
    return requiredPower <= activePower;
  }
}

export const useProjectPermissions = (): ProjectPermissions => {
  const project = useProject();
  const isAdmin = useIsAdmin();
  const type =
    ProjectPermissionType[
      project.computedPermissions.type as NonNullable<
        typeof project.computedPermissions.type
      >
    ];
  return new ProjectPermissions(
    type,
    project.computedPermissions.permittedLanguageIds,
    isAdmin
  );
};
