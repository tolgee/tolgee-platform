import { useTranslate } from '@tolgee/react';

export const useOrgRoleTranslation = () => {
  const { t } = useTranslate();

  return (permission: string, hint?: boolean) => {
    const type = permission.toUpperCase();
    if (hint) {
      switch (type) {
        case 'MEMBER':
          return t('organization_role_type_MEMBER_hint');
        case 'OWNER':
          return t('organization_role_type_OWNER_hint');
      }
    } else {
      switch (type) {
        case 'MEMBER':
          return t('organization_role_type_MEMBER');
        case 'OWNER':
          return t('organization_role_type_OWNER');
      }
    }
    return type;
  };
};
