import { ComponentProps, default as React, FunctionComponent } from 'react';
import { MenuItem } from '@mui/material';

import { projectPermissionTypes } from 'tg.constants/projectPermissionTypes';
import { usePermissionTranslation } from 'translationTools/usePermissionTranslation';

import { Select } from '../common/form/fields/Select';

export const PermissionSelect: FunctionComponent<
  ComponentProps<typeof Select>
> = (props) => {
  const translatePermission = usePermissionTranslation();
  return (
    <Select {...props} renderValue={(v) => translatePermission(v)}>
      {Object.keys(projectPermissionTypes).map((k) => (
        <MenuItem data-cy="permission-select-item" key={k} value={k}>
          {translatePermission(k)}
        </MenuItem>
      ))}
    </Select>
  );
};
