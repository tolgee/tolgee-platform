import { List } from '@mui/material';
import { PermissionsRole } from './PermissionsRole';

import {
  HierarchyItem,
  PermissionBasicState,
  PermissionModelRole,
  RolesMap,
} from './types';

type Props = {
  dependencies: HierarchyItem;
  state: PermissionBasicState;
  onChange: (value: PermissionBasicState) => void;
  roles: RolesMap;
};

export const PermissionsBasic: React.FC<Props> = ({
  dependencies,
  state,
  onChange,
  roles,
}) => {
  const rolesList = Object.keys(roles);

  return (
    <List>
      {rolesList.map((role) => {
        return (
          <PermissionsRole
            key={role}
            role={role as NonNullable<PermissionModelRole>}
            state={state}
            scopes={roles[role]}
            onChange={onChange}
            dependencies={dependencies}
          />
        );
      })}
    </List>
  );
};
