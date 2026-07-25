import {
  LanguagePermissions,
  PermissionModelRole,
  PermissionModelScope,
  PermissionSettingsState,
} from 'tg.component/PermissionsSettings/types';
import { scopesToLanguagePermissions } from 'tg.component/PermissionsSettings/hierarchyTools';
import { languagePermissionsForRole } from './permissionBody';

export type InvitationPermissionsBody = (
  | { scopes: PermissionModelScope[] }
  | { type: PermissionModelRole }
) &
  LanguagePermissions;

export type GetInvitationPermissionBodyResult =
  | { ok: true; body: InvitationPermissionsBody }
  | { ok: false; reason: 'empty-scopes' | 'invalid' };

export function getInvitationPermissionBody(
  permissions: PermissionSettingsState
): GetInvitationPermissionBodyResult {
  if (permissions.tab === 'advanced' && permissions.advancedState.scopes) {
    const { advancedState } = permissions;
    const { scopes } = advancedState;
    if (scopes.length === 0) {
      return { ok: false, reason: 'empty-scopes' };
    }
    return {
      ok: true,
      body: { scopes, ...scopesToLanguagePermissions(advancedState) },
    };
  }
  if (permissions.tab === 'basic' && permissions.basicState.role) {
    const role = permissions.basicState.role;
    return {
      ok: true,
      body: {
        type: role,
        ...languagePermissionsForRole(role, permissions.basicState.languages),
      },
    };
  }
  return { ok: false, reason: 'invalid' };
}
