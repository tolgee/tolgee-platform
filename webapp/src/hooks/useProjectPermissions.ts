import { useProject } from './useProject';
import { getPermissionTools } from '../fixtures/getPermissionTools';
import { useUser } from 'tg.globalContext/helpers';

export const useProjectPermissions = () => {
  const project = useProject();
  const userInfo = useUser();
  return getPermissionTools(project.computedPermission, userInfo);
};
