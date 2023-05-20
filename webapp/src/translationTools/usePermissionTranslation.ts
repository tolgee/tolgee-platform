import { useTranslate } from '@tolgee/react';

export const usePermissionTranslation = () => {
  const { t } = useTranslate();

  return (permission: string, hint?: boolean) => {
    const type = permission?.toUpperCase();
    if (hint) {
      switch (type) {
        case 'MANAGE':
          return t('permission_type_manage_hint');
        case 'EDIT':
          return t('permission_type_edit_hint');
        case 'TRANSLATE':
          return t('permission_type_translate_hint');
        case 'VIEW':
          return t('permission_type_view_hint');
        case 'GRANULAR':
          return t('permission_type_granular_hint');
      }
    } else {
      switch (type) {
        case 'MANAGE':
          return t('permission_type_manage');
        case 'EDIT':
          return t('permission_type_edit');
        case 'TRANSLATE':
          return t('permission_type_translate');
        case 'VIEW':
          return t('permission_type_view');
        case 'GRANULAR':
          return t('permission_type_granular');
      }
    }
    return type;
  };
};
