import { Box, styled, alpha, Typography } from '@mui/material';
import clsx from 'clsx';
import { ALL_LANGUAGES_SCOPES } from 'tg.ee/PermissionsAdvanced/hierarchyTools';
import { stopAndPrevent } from 'tg.fixtures/eventHandler';
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
`;

const StyledTypography = styled(Typography)`
  font-weight: 500;
  font-size: 15px;

  &.selected {
    color: ${({ theme }) => theme.palette.primary.main};
  }
`;

type Props = {
  role: NonNullable<PermissionModelRole>;
  state: PermissionBasicState;
  scopes: PermissionModelScope[];
  onChange: (value: PermissionBasicState) => void;
  allLangs?: LanguageModel[];
};

export const PermissionsRole: React.FC<Props> = ({
  state,
  role,
  onChange,
  scopes,
  allLangs,
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
            />
          </Box>
        )}
      </Box>
    </StyledListItem>
  );
};
