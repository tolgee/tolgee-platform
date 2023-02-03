import { styled, Checkbox, FormControlLabel } from '@mui/material';
import {
  HierarchyItem,
  HierarchyType,
  LanguagePermissions,
  PermissionAdvanced,
  PermissionModelScope,
} from 'tg.component/PermissionsSettings/types';
import { LanguagePermissionsMenu } from 'tg.component/security/LanguagePermissionsMenu';
import { useProjectLanguages } from 'tg.hooks/useProjectLanguages';
import {
  checkChildren,
  getChildScopes,
  getMinimalLanguages,
  getScopeLanguagePermission,
  updateByDependencies,
  getBlockingScopes,
} from './hierarchyTools';
import { useScopeTranslations } from './useScopeTranslations';

const StyledContainer = styled('div')`
  display: grid;
  justify-content: start;
`;

const StyledChildren = styled('div')`
  display: grid;
  margin-left: 32px;
`;

const StyledRow = styled('div')`
  display: grid;
  grid-template-columns: minmax(150px, auto) auto;
  gap: 16px;
  align-items: center;
`;

type Props = {
  dependencies: HierarchyItem;
  structure: HierarchyType;
  state: PermissionAdvanced;
  onChange: (value: PermissionAdvanced) => void;
};

export const Hierarchy: React.FC<Props> = ({
  dependencies,
  structure,
  state,
  onChange,
}) => {
  const languages = useProjectLanguages();
  const allLangs = languages.map((l) => l.id);
  const { scopes } = state;
  const scopeIncluded = structure.value && scopes.includes(structure.value);
  const { childrenCheckedSome, childrenCheckedAll } = checkChildren(
    structure,
    scopes
  );

  const updateScopes = (scopes: PermissionModelScope[], value: boolean) => {
    let newScopes = [...state.scopes];
    scopes.forEach((scope) => {
      const exists = newScopes.includes(scope);
      if (exists && value === false) {
        newScopes = newScopes.filter((s) => s !== scope);
      } else if (!exists && value === true) {
        newScopes = [...newScopes, scope];
      }
    });
    return newScopes;
  };

  const updateLanguagePermissions = (
    languagePermission: keyof LanguagePermissions,
    value: number[]
  ) => {
    onChange(
      updateByDependencies(
        myScopes,
        {
          ...state,
          [languagePermission]: value,
        },
        dependencies,
        allLangs
      )
    );
  };

  // scopes the item is responsible for
  const myScopes = structure.value
    ? [structure.value]
    : getChildScopes(structure);

  // check if all dependant scopes are in responsible nodes
  // meaning if we toggle this, nothing outside gets broken
  const blockingScopes = getBlockingScopes(myScopes, scopes, dependencies);

  const blockedLanguages = getMinimalLanguages(blockingScopes, state);

  const disabled = Boolean(blockingScopes.length);

  const fullyChecked =
    scopeIncluded || (!structure.value && childrenCheckedAll);
  const halfChecked = !structure.value && !fullyChecked && childrenCheckedSome;

  const handleClick = () => {
    if (fullyChecked) {
      onChange({
        ...state,
        scopes: updateScopes(myScopes, false),
      });
    } else {
      // get myScopes and also their required scopes
      onChange(updateByDependencies(myScopes, state, dependencies, allLangs));
    }
  };

  const { getScopeTranslation } = useScopeTranslations();

  const label =
    structure.label ||
    (structure.value ? getScopeTranslation(structure.value) : undefined);

  const languagePermission = getScopeLanguagePermission(structure.value);

  return (
    <StyledContainer>
      <StyledRow>
        <FormControlLabel
          control={
            <Checkbox
              checked={fullyChecked}
              indeterminate={halfChecked}
              onClick={handleClick}
              disabled={disabled}
            />
          }
          label={label}
        />

        {languagePermission && (
          <LanguagePermissionsMenu
            buttonProps={{ size: 'small' }}
            disabled={!fullyChecked || blockedLanguages}
            selected={state[languagePermission] || []}
            onSelect={(value) =>
              updateLanguagePermissions(languagePermission, value)
            }
          />
        )}
      </StyledRow>
      <StyledChildren>
        {structure.children?.map((child) => {
          return (
            <Hierarchy
              key={child.value}
              dependencies={dependencies}
              structure={child}
              state={state}
              onChange={onChange}
            />
          );
        })}
      </StyledChildren>
    </StyledContainer>
  );
};
