import { useTranslate } from '@tolgee/react';

export const translatedPermissionType = (type: string, noWrap = false) => {
  const { t } = useTranslate();

  return t(`permission_type_${type.toLowerCase()}`, { noWrap });
};
