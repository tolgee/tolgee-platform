import { styled, Checkbox, FormControlLabel } from '@mui/material';
import { HierarchyType, PermissionModelScope } from '../types';
import { checkChildren } from './tools';

const StyledContainer = styled('div')`
  display: grid;
`;

const StyledChildren = styled('div')`
  display: grid;
  margin-left: 32px;
`;

type Props = {
  structure: HierarchyType;
  scopes: PermissionModelScope[];
  setScope: (scope: PermissionModelScope, value: boolean) => void;
};

export const Hierarchy: React.FC<Props> = ({ structure, scopes, setScope }) => {
  const scopeIncluded = structure.value && scopes.includes(structure.value);
  const { childrenCheckedSome, childrenCheckedAll } = checkChildren(
    structure,
    scopes
  );

  const fullyChecked =
    scopeIncluded || (!structure.value && childrenCheckedAll);
  const halfChecked = !fullyChecked && childrenCheckedSome;

  const handleClick = () => {
    if (structure.value) {
      setScope(structure.value, !scopeIncluded);
    }
  };

  return (
    <StyledContainer>
      <FormControlLabel
        control={
          <Checkbox
            checked={fullyChecked}
            indeterminate={halfChecked}
            onClick={handleClick}
          />
        }
        label={structure.label}
      />
      <StyledChildren>
        {structure.children?.map((child) => {
          return (
            <Hierarchy
              key={child.value}
              structure={child}
              scopes={scopes}
              setScope={setScope}
            />
          );
        })}
      </StyledChildren>
    </StyledContainer>
  );
};
