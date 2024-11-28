import { Checkbox, FormControlLabel, styled, Tooltip } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import {
  HierarchyItem,
  HierarchyType,
  LanguageModel,
  PermissionAdvancedState,
  PermissionModelScope,
} from './types';
import { LanguagePermissionsMenu } from '../security/LanguagePermissionsMenu';
import {
  ALL_LANGUAGES_SCOPES,
  checkChildren,
  getBlockingScopes,
  getChildScopes,
  getLanguagesUnion,
  getScopeLanguagePermission,
  isAllLanguages,
  updateByDependencies,
  updateByDependenciesSoftly,
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
  state: PermissionAdvancedState;
  onChange: (value: PermissionAdvancedState) => void;
  allLangs?: LanguageModel[];
};

export const Hierarchy: React.FC<Props> = ({
  dependencies,
  structure,
  state,
  onChange,
  allLangs,
}) => {
  const { t } = useTranslate();
  const allLangIds = allLangs?.map((l) => l.id) || [];
  const { scopes } = state;
  const scopeIncluded = structure.value && scopes.includes(structure.value);
  const { childrenCheckedSome, childrenCheckedAll } = checkChildren(
    structure,
    scopes
  );

  // scopes the item is responsible for
  const myScopes = structure.value
    ? [structure.value]
    : getChildScopes(structure);

  const myLanguageProps = myScopes
    .map(getScopeLanguagePermission)
    .filter(Boolean);

  // check if all dependant scopes are in responsible nodes
  // meaning if we toggle this, nothing outside gets broken
  const blockingScopes = getBlockingScopes(myScopes, scopes, dependencies);

  const blockedLanguages = getLanguagesUnion(blockingScopes, state, allLangIds);

  const disabled = Boolean(blockingScopes.length);

  const fullyChecked =
    scopeIncluded || (!structure.value && childrenCheckedAll);
  const halfChecked = !structure.value && !fullyChecked && childrenCheckedSome;

  const fullyBlocked = Boolean(
    blockingScopes.find((scope) => ALL_LANGUAGES_SCOPES.includes(scope))
  );

  const { getScopeTranslation } = useScopeTranslations();

  const label =
    structure.label ||
    (structure.value ? getScopeTranslation(structure.value) : undefined);

  const minimalLanguages = getLanguagesUnion(
    structure.value
      ? [structure.value]
      : myScopes.filter((sc) => scopes.includes(sc)),
    state,
    []
  );

  const displayLanguages = Boolean(structure.value && myLanguageProps.length);

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

  const updateLanguagePermissions = (value: number[]) => {
    const affectedScopes = myScopes.filter((sc) => scopes.includes(sc));
    const newState = {
      ...state,
    };
    myLanguageProps.forEach((langProp) => {
      newState[langProp!] = value;
    });
    onChange(
      updateByDependencies(affectedScopes, newState, dependencies, allLangIds)
    );
  };

  const handleToggle = () => {
    if (fullyChecked) {
      onChange({
        ...state,
        scopes: updateScopes(myScopes, false),
      });
    } else {
      // get myScopes and also their required scopes
      if (structure.value && ALL_LANGUAGES_SCOPES.includes(structure.value)) {
        onChange(
          updateByDependencies(myScopes, state, dependencies, allLangIds)
        );
      } else {
        onChange(
          updateByDependenciesSoftly(myScopes, state, dependencies, allLangIds)
        );
      }
    }
  };

  return (
    <StyledContainer>
      <StyledRow>
        <Tooltip
          key={structure.value}
          enterDelay={1000}
          enterNextDelay={1000}
          title={
            (disabled && structure.value
              ? t('permissions_advanced_item_blocked', {
                  scopes: blockingScopes.join(', '),
                })
              : structure.value) || ''
          }
          placement="left"
          disableInteractive
        >
          <FormControlLabel
            permissions-scope={structure.value}
            data-cy="permissions-advanced-item"
            control={
              <Checkbox
                data-cy="permissions-advanced-checkbox"
                size="small"
                style={{ paddingTop: 4, paddingBottom: 4 }}
                checked={fullyChecked}
                indeterminate={halfChecked}
                onClick={handleToggle}
                disabled={disabled}
              />
            }
            label={label}
          />
        </Tooltip>

        {minimalLanguages && displayLanguages && allLangs && (
          <LanguagePermissionsMenu
            buttonProps={{ size: 'small', style: { minWidth: 180 } }}
            disabled={
              fullyBlocked ||
              (!halfChecked && !fullyChecked) ||
              (blockedLanguages !== false && isAllLanguages(blockedLanguages)
                ? allLangIds
                : blockedLanguages)
            }
            allLanguages={allLangs}
            selected={minimalLanguages}
            onSelect={(value) => updateLanguagePermissions(value)}
          />
        )}
      </StyledRow>
      <StyledChildren>
        {structure.children?.map((child, i) => {
          return (
            <Hierarchy
              key={`${child.value}.${i}`}
              dependencies={dependencies}
              structure={child}
              state={state}
              onChange={onChange}
              allLangs={allLangs}
            />
          );
        })}
      </StyledChildren>
    </StyledContainer>
  );
};
