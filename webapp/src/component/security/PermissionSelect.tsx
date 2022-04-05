import { ComponentProps, default as React, FunctionComponent } from 'react';
import { MenuItem } from '@mui/material';

import { projectPermissionTypes } from 'tg.constants/projectPermissionTypes';
import { translatedPermissionType } from 'tg.fixtures/translatePermissionFile';

import { Select } from '../common/form/fields/Select';

export const PermissionSelect: FunctionComponent<
  ComponentProps<typeof Select>
> = (props) => {
  return (
    <Select {...props} renderValue={(v) => translatedPermissionType(v)}>
      {Object.keys(projectPermissionTypes).map((k) => (
        <MenuItem data-cy="permission-select-item" key={k} value={k}>
          {translatedPermissionType(k)}
        </MenuItem>
      ))}
    </Select>
  );
};
