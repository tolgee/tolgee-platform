import { useTranslate } from '@tolgee/react';
import { PermissionModelRole } from 'tg.component/PermissionsSettings/types';

type Role = NonNullable<PermissionModelRole> | 'ADVANCED';

export const useRoleTranslations = () => {
  const { t } = useTranslate();

  const labels: Record<Role, { label: string; hint?: string }> = {
    NONE: {
      label: t('permission_type_none'),
      hint: t('permission_type_none_hint'),
    },
    VIEW: {
      label: t('permission_type_view'),
      hint: t('permission_type_view_hint'),
    },
    TRANSLATE: {
      label: t('permission_type_translate'),
      hint: t('permission_type_translate_hint'),
    },
    REVIEW: {
      label: t('permission_type_review'),
      hint: t('permission_type_review_hint'),
    },
    EDIT: {
      label: t('permission_type_edit'),
      hint: t('permission_type_edit_hint'),
    },
    MANAGE: {
      label: t('permission_type_manage'),
      hint: t('permission_type_manage_hint'),
    },
    ADVANCED: {
      label: t('permission_type_granular'),
      hint: t('permission_type_granular_hint'),
    },
  };

  return {
    getRoleTranslation: (role: Role) => labels[role].label,
    getRoleHint: (role: Role) => labels[role].hint,
  };
};
