import { components, operations } from 'tg.service/apiSchema.generated';

export type PermissionModel = components['schemas']['PermissionModel'];

export type PermissionBasic =
  operations['setUsersPermissions_1']['parameters']['query'] & {
    role: PermissionModel['type'];
  };
