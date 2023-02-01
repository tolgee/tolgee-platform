import { useTranslate } from '@tolgee/react';
import { PermissionModelRole } from 'tg.component/PermissionsSettings/types';

type Role = NonNullable<PermissionModelRole> | 'ADVANCED';

export const useRoleTranslations = () => {
  const { t } = useTranslate();

  const labels: Record<Role, string> = {
    NONE: t('permission_type_none'),
    VIEW: t('permission_type_view'),
    TRANSLATE: t('permission_type_translate'),
    REVIEW: t('permission_type_review'),
    EDIT: t('permission_type_edit'),
    MANAGE: t('permission_type_manage'),
    ADVANCED: t('permission_type_advanced'),
  };

  return {
    getRoleTranslation: (role: Role) => labels[role],
  };
};
