import MenuItem from '@material-ui/core/MenuItem';
import { ComponentProps, default as React, FunctionComponent } from 'react';
import { T } from '@tolgee/react';
import { Select } from 'tg.component/common/form/fields/Select';
import { OrganizationRoleType } from 'tg.service/response.types';

export const OrganizationRoleSelect: FunctionComponent<
  ComponentProps<typeof Select>
> = (props) => {
  return (
    <Select
      {...props}
      renderValue={(v) => (
        <T>{`organization_role_type_${OrganizationRoleType[v]}`}</T>
      )}
    >
      {Object.keys(OrganizationRoleType).map((k) => (
        <MenuItem data-cy="organization-role-select-item" key={k} value={k}>
          <T>{`organization_role_type_${OrganizationRoleType[k]}`}</T>
        </MenuItem>
      ))}
    </Select>
  );
};
