import { useTranslate } from '@tolgee/react';

export const useOrgRoleTranslation = () => {
  const { t } = useTranslate();

  return (permission: string, hint?: boolean) => {
    const type = permission.toUpperCase();
    if (hint) {
      switch (type) {
        case 'MEMBER':
          return t('organization_role_type_member_hint');
        case 'OWNER':
          return t('organization_role_type_owner_hint');
      }
    } else {
      switch (type) {
        case 'MEMBER':
          return t('organization_role_type_member');
        case 'OWNER':
          return t('organization_role_type_owner');
      }
    }
    return type;
  };
};
