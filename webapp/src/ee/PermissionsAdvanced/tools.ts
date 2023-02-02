import {
  HierarchyItem,
  HierarchyType,
  LanguagePermissions,
  PermissionAdvanced,
  PermissionModelScope,
} from 'tg.component/PermissionsSettings/types';

export const getChildScopes = (structure: HierarchyType) => {
  let result: PermissionModelScope[] = [];
  structure.children?.forEach((child) => {
    if (child.value) {
      result.push(child.value);
    }
    result = [...result, ...getChildScopes(child)];
  });
  return result;
};

export const checkChildren = (
  structure: HierarchyType,
  scopes: PermissionModelScope[]
) => {
  const childrenScopes = getChildScopes(structure);
  let childrenCheckedAll = childrenScopes.length > 0;
  let childrenCheckedSome = false;
  childrenScopes.forEach((scope) => {
    if (scopes.includes(scope)) {
      childrenCheckedSome = true;
    } else {
      childrenCheckedAll = false;
    }
  });
  return { childrenCheckedAll, childrenCheckedSome };
};

export const findRequired = (
  scope: PermissionModelScope,
  dependencies: HierarchyItem
) => {
  let result: PermissionModelScope[] = [];
  if (dependencies.scope === scope) {
    dependencies.requires.forEach((required) => {
      result = [
        ...result,
        required.scope,
        ...findRequired(required.scope, required),
      ];
    });
  } else {
    dependencies.requires.forEach((required) => {
      result = [...result, ...findRequired(scope, required)];
    });
  }
  return result;
};

export const getDependentScopes = (
  myScopes: PermissionModelScope[],
  dependencies: HierarchyItem
) => {
  const result = new Set<PermissionModelScope>();

  dependencies.requires.forEach((item) => {
    const childItems = getDependentScopes(myScopes, item);
    if (myScopes.includes(item.scope) || childItems.length) {
      result.add(dependencies.scope);
      childItems.forEach((s) => result.add(s));
    }
  });

  return Array.from(result);
};

export const getRequiredScopes = (
  scopes: PermissionModelScope[],
  dependencies: HierarchyItem
) => {
  const result = new Set<PermissionModelScope>();
  scopes.forEach((scope) => {
    result.add(scope);
    findRequired(scope, dependencies).forEach((scope) => result.add(scope));
  });
  return Array.from(result);
};

const SCOPE_TO_LANG_PROPERTY_MAP = {
  'translations.view': 'viewLanguages',
  'translations.edit': 'translateLanguages',
  'translations.state-edit': 'stateChangeLanguages',
};

export const getScopeLanguagePermission = (
  scope: PermissionModelScope | undefined
) => {
  return scope && SCOPE_TO_LANG_PROPERTY_MAP[scope];
};

export const getMinimalLanguages = (
  scopes: PermissionModelScope[],
  permittedLanguages: LanguagePermissions
) => {
  const result = new Set<number>();
  let all = false;
  scopes.forEach((scope) => {
    const languageProp = getScopeLanguagePermission(scope);
    const dependantLanguages = permittedLanguages[languageProp];
    dependantLanguages?.forEach((langId) => result.add(langId));
    if (dependantLanguages?.length === 0) {
      all = true;
    }
  });
  return all ? [] : result.size ? Array.from(result) : false;
};

export const updateByDependencies = (
  myScopes: PermissionModelScope[],
  currentState: PermissionAdvanced,
  dependencies: HierarchyItem,
  allLangs: number[]
) => {
  const newState = {
    ...currentState,
    scopes: Array.from(new Set([...currentState.scopes, ...myScopes])),
  };
  myScopes.forEach((myScope) => {
    const minimalLanguages = getMinimalLanguages([myScope], newState);
    getRequiredScopes([myScope], dependencies).forEach((requiredScope) => {
      if (!newState.scopes.includes(requiredScope)) {
        newState.scopes = [...newState.scopes, requiredScope];
      }
      const languageProp = getScopeLanguagePermission(requiredScope);
      if (minimalLanguages && languageProp && requiredScope !== myScope) {
        if (
          newState[languageProp].length === 0 ||
          minimalLanguages.length === 0
        ) {
          newState[languageProp] = allLangs;
        } else {
          minimalLanguages.forEach((l) => {
            if (!newState[languageProp]?.includes(l)) {
              newState[languageProp] = [...(newState[languageProp] || []), l];
            }
          });
        }
      }
    });
  });
  return newState;
};
