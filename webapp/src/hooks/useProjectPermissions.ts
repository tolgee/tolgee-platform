import { ProjectPermissionType } from '../service/response.types';
import { useProject } from './useProject';

export class ProjectPermissions {
  constructor(private activePermission: ProjectPermissionType) {}

  private static readonly ORDERED_PERMISSIONS = [
    ProjectPermissionType.VIEW,
    ProjectPermissionType.TRANSLATE,
    ProjectPermissionType.EDIT,
    ProjectPermissionType.MANAGE,
  ];

  satisfiesPermission(type: ProjectPermissionType) {
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
  const type =
    ProjectPermissionType[
      project.computedPermissions as NonNullable<
        typeof project.computedPermissions
      >
    ];
  return new ProjectPermissions(type);
};
