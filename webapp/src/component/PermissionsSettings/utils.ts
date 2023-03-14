import { PermissionModel } from 'tg.component/PermissionsSettings/types';

export function getLanguagesByRole(permissions: PermissionModel) {
  if (permissions.type === 'TRANSLATE') {
    return permissions.translateLanguageIds || [];
  }
  if (permissions.type === 'REVIEW') {
    return permissions.stateChangeLanguageIds || [];
  }
  return null;
}
