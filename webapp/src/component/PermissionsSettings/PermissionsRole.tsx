import { ListItemButton, Box } from '@mui/material';
import { stopAndPrevent } from 'tg.fixtures/eventHandler';
import { RoleLanguages } from './RoleLanguages';

import {
  HierarchyItem,
  PermissionState,
  PermissionModelRole,
  PermissionModelScope,
} from './types';
import { useRoleTranslations } from './useRoleTranslations';

type Props = {
  dependencies: HierarchyItem;
  role: NonNullable<PermissionModelRole>;
  state: PermissionState;
  scopes: PermissionModelScope[];
  onChange: (value: PermissionState) => void;
};

export const PermissionsRole: React.FC<Props> = ({
  dependencies,
  state,
  role,
  onChange,
  scopes,
}) => {
  const handleSelect = () => {
    if (role !== state.role) {
      onChange({ ...state, role, scopes });
    }
  };
  const { getRoleTranslation, getRoleHint } = useRoleTranslations();
  const selected = state.role === role;

  return (
    <ListItemButton selected={selected} onClick={handleSelect}>
      <Box>
        <Box>{getRoleTranslation(role)}</Box>
        <Box>{getRoleHint(role)}</Box>
        {selected && (
          <Box onMouseDown={stopAndPrevent()} onClick={stopAndPrevent()}>
            <RoleLanguages
              state={state}
              onChange={onChange}
              dependencies={dependencies}
            />
          </Box>
        )}
      </Box>
    </ListItemButton>
  );
};
