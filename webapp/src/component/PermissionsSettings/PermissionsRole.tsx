import { ListItemButton, Box, useTheme, styled, alpha } from '@mui/material';
import {
  ALL_LANGUAGES_SCOPES,
  updateByDependencies,
} from 'tg.ee/PermissionsAdvanced/hierarchyTools';
import { stopAndPrevent } from 'tg.fixtures/eventHandler';
import { useProjectLanguages } from 'tg.hooks/useProjectLanguages';
import { RoleLanguages } from './RoleLanguages';

import {
  HierarchyItem,
  PermissionState,
  PermissionModelRole,
  PermissionModelScope,
} from './types';
import { useRoleTranslations } from './useRoleTranslations';

const StyledListItem = styled(ListItemButton)`
  position: relative;
  background-color: unset;
  &:active {
    background-color: ${({ theme }) => alpha(theme.palette.primary.main, 0.16)};
  }
`;

const StyledShield = styled('div')`
  position: absolute;
  top: 0px;
  left: 0px;
  right: 0px;
  bottom: 0px;
`;

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
  const theme = useTheme();
  const allLangs = useProjectLanguages().map((l) => l.id);
  const handleSelect = () => {
    if (role !== state.role) {
      onChange(
        updateByDependencies(
          scopes,
          { ...state, role, scopes },
          dependencies,
          allLangs
        )
      );
    }
  };
  const { getRoleTranslation, getRoleHint } = useRoleTranslations();
  const selected = state.role === role;
  const displayLanguages = !scopes.find((scope) =>
    ALL_LANGUAGES_SCOPES.includes(scope)
  );

  return (
    <StyledListItem
      selected={selected}
      onClick={handleSelect}
      style={{
        cursor: selected ? 'default' : 'pointer',
        backgroundColor: selected
          ? alpha(theme.palette.primary.main, 0.16)
          : undefined,
      }}
    >
      {selected && (
        <StyledShield
          onMouseEnter={stopAndPrevent()}
          onClick={stopAndPrevent()}
          onMouseDown={stopAndPrevent()}
        />
      )}
      <Box>
        <Box>{getRoleTranslation(role)}</Box>
        <Box>{getRoleHint(role)}</Box>
        {selected && displayLanguages && (
          <Box onMouseDown={stopAndPrevent()} onClick={stopAndPrevent()}>
            <RoleLanguages
              state={state}
              onChange={onChange}
              dependencies={dependencies}
            />
          </Box>
        )}
      </Box>
    </StyledListItem>
  );
};
