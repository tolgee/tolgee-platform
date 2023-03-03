import { Box, styled, alpha, Typography } from '@mui/material';
import clsx from 'clsx';
import { ALL_LANGUAGES_SCOPES } from 'tg.ee/PermissionsAdvanced/hierarchyTools';
import { stopAndPrevent } from 'tg.fixtures/eventHandler';
import { RoleLanguages } from './RoleLanguages';

import {
  HierarchyItem,
  PermissionBasicState,
  PermissionModelRole,
  PermissionModelScope,
} from './types';
import { useRoleTranslations } from './useRoleTranslations';

const StyledListItem = styled('div')`
  position: relative;
  background-color: unset;
  padding: ${({ theme }) => theme.spacing(1, 1.5)};
  cursor: pointer;
  border: 1px solid transparent;
  border-radius: 3px;

  transition: background-color 0.2s ease-in, border-color 0.05s ease-in;

  &:hover {
    background-color: ${({ theme }) => alpha(theme.palette.emphasis[100], 0.8)};
  }

  &.selected {
    background-color: ${({ theme }) => theme.palette.emphasis[100]};
    border-color: ${({ theme }) => theme.palette.divider2.main};
  }
`;

const StyledTypography = styled(Typography)`
  font-weight: 500;
  font-size: 15px;

  &.selected {
    color: ${({ theme }) => theme.palette.primary.main};
  }
`;

type Props = {
  dependencies: HierarchyItem;
  role: NonNullable<PermissionModelRole>;
  state: PermissionBasicState;
  scopes: PermissionModelScope[];
  onChange: (value: PermissionBasicState) => void;
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
      onChange({
        ...state,
        role,
      });
    }
  };
  const { getRoleTranslation, getRoleHint } = useRoleTranslations();
  const selected = state.role === role;
  const displayLanguages = !scopes.find((scope) =>
    ALL_LANGUAGES_SCOPES.includes(scope)
  );

  return (
    <StyledListItem onClick={handleSelect} className={clsx({ selected })}>
      <Box>
        <StyledTypography className={clsx({ selected })}>
          {getRoleTranslation(role)}
        </StyledTypography>
        <Box>{getRoleHint(role)}</Box>
        {selected && displayLanguages && (
          <Box
            zIndex={1}
            onMouseDown={stopAndPrevent()}
            onClick={stopAndPrevent()}
          >
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
