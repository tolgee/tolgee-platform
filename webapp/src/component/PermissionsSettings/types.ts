import { components, operations } from 'tg.service/apiSchema.generated';

export type TabsType = 'basic' | 'advanced';

export type PermissionModel = components['schemas']['PermissionModel'];

export type PermissionModelRole = PermissionModel['type'];

export type PermissionModelScope = PermissionModel['scopes'][0];

export type LanguagePermissions =
  operations['setUsersPermissions_1']['parameters']['query'];

export type PermissionState = {
  role?: PermissionModelRole;
  scopes: PermissionModelScope[];
} & LanguagePermissions;

export type PermissionSettingsState = {
  tab: TabsType;
  state: PermissionState;
};

export type HierarchyType = {
  label?: string;
  value?: PermissionModelScope;
  children?: HierarchyType[];
};

export type RolesMap = Record<
  NonNullable<PermissionModelRole>,
  PermissionModelScope[]
>;

export type HierarchyItem = components['schemas']['HierarchyItem'];
