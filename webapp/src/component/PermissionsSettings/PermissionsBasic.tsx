import { List } from '@mui/material';
import { PermissionsRole } from './PermissionsRole';

import {
  LanguageModel,
  PermissionBasicState,
  PermissionModelRole,
  RolesMap,
} from './types';

type Props = {
  state: PermissionBasicState;
  onChange: (value: PermissionBasicState) => void;
  roles: RolesMap;
  allLangs?: LanguageModel[];
  hideNone?: boolean;
  disabled?: boolean;
};

export const PermissionsBasic: React.FC<Props> = ({
  state,
  onChange,
  roles,
  allLangs,
  hideNone,
  disabled,
}) => {
  const rolesList = Object.keys(roles).filter(
    (role) => role !== 'NONE' || !hideNone
  );

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
            allLangs={allLangs}
            disabled={disabled}
          />
        );
      })}
    </List>
  );
};
