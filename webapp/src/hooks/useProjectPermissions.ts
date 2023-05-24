import { useProject } from './useProject';
import { getPermissionTools } from '../fixtures/getPermissionTools';
export const useProjectPermissions = () => {
  const project = useProject();
  return getPermissionTools(project.computedPermission);
};
