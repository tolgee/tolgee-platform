import { components, operations } from 'tg.service/apiSchema.generated';

export type TabsType = 'basic' | 'advanced';

export type PermissionModel = components['schemas']['PermissionModel'];

export type PermissionModelRole = PermissionModel['type'];

export type PermissionModelScope = PermissionModel['scopes'][0];

export type PermissionBasic =
  operations['setUsersPermissions_1']['parameters']['query'] & {
    role: PermissionModelRole;
  };

export type PermissionAdvanced = {
  scopes: PermissionModelScope[];
};

export type PermissionSettingsState = {
  tab: TabsType;
  basic: PermissionBasic;
};

export type HierarchyType = {
  label: string;
  value?: PermissionModelScope;
  children?: HierarchyType[];
};

export type HierarchyItem = components['schemas']['HierarchyItem'];
