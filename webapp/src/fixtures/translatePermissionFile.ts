import { useTranslate } from '@tolgee/react';

import { projectPermissionTypes } from '../constants/projectPermissionTypes';

export const translatedPermissionType = (type: string, noWrap = false) => {
  const { t } = useTranslate();

  return t(`permission_type_${projectPermissionTypes[type]}`, {
    noWrap,
  });
};
