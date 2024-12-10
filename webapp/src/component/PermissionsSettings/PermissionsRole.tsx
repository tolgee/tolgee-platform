import { alpha, Box, styled, Typography } from '@mui/material';
import clsx from 'clsx';
import { stopAndPrevent } from 'tg.fixtures/eventHandler';
import { ALL_LANGUAGES_SCOPES } from './hierarchyTools';
import { RoleLanguages } from './RoleLanguages';

import {
  LanguageModel,
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
    background-color: ${({ theme }) => alpha(theme.palette.emphasis[50], 0.8)};
  }

  &.selected {
    background-color: ${({ theme }) => theme.palette.emphasis[50]};
    border-color: ${({ theme }) => theme.palette.divider1};
  }

  &.disabled {
    cursor: default;
    color: ${({ theme }) => theme.palette.text.disabled};
    background-color: unset;
  }
`;

const StyledTypography = styled(Typography)`
  font-weight: 500;
  font-size: 15px;

  &.selected {
    color: ${({ theme }) => theme.palette.primary.main};
  }

  &.disabled {
    color: ${({ theme }) => theme.palette.text.disabled};
  }
`;

type Props = {
  role: NonNullable<PermissionModelRole>;
  state: PermissionBasicState;
  scopes: PermissionModelScope[];
  onChange: (value: PermissionBasicState) => void;
  allLangs?: LanguageModel[];
  disabled?: boolean;
};

export const PermissionsRole: React.FC<Props> = ({
  state,
  role,
  onChange,
  scopes,
  allLangs,
  disabled,
}) => {
  const handleSelect = () => {
    if (role !== state.role && !disabled) {
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
    <StyledListItem
      onClick={handleSelect}
      className={clsx({ selected, disabled })}
    >
      <Box>
        <StyledTypography className={clsx({ selected, disabled })}>
          {getRoleTranslation(role)}
        </StyledTypography>
        <Box>{getRoleHint(role)}</Box>
        {selected && displayLanguages && allLangs && (
          <Box
            zIndex={1}
            onMouseDown={stopAndPrevent()}
            onClick={stopAndPrevent()}
          >
            <RoleLanguages
              state={state}
              onChange={onChange}
              allLangs={allLangs}
              disabled={disabled}
            />
          </Box>
        )}
      </Box>
    </StyledListItem>
  );
};
