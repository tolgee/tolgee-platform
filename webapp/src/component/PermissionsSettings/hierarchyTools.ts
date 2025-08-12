import {
  HierarchyItem,
  HierarchyType,
  LanguagePermissions,
  PermissionAdvancedState,
  PermissionModelScope,
} from './types';

export const ALL_LANGUAGES_SCOPES = ['admin'];

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

export const getBlockingScopes = (
  myScopes: PermissionModelScope[],
  selectedScopes: PermissionModelScope[],
  dependencies: HierarchyItem
) => {
  const dependentScopes = getDependentScopes(myScopes, dependencies);
  return dependentScopes.filter(
    (dependentScope) =>
      selectedScopes.includes(dependentScope) &&
      !myScopes.includes(dependentScope)
  );
};

export const getRequiredScopes = (
  scope: PermissionModelScope,
  dependencies: HierarchyItem
) => {
  const result = new Set<PermissionModelScope>();
  result.add(scope);
  findRequired(scope, dependencies).forEach((scope) => result.add(scope));
  return Array.from(result);
};

const SCOPE_TO_LANG_PROPERTY_MAP = {
  'translations.view': 'viewLanguages',
  'translations.edit': 'translateLanguages',
  'translations.state-edit': 'stateChangeLanguages',
  'translations.suggest': 'suggestLanguages',
} as const;

type ValueOf<T> = T[keyof T];

export type LanguageProperty = ValueOf<typeof SCOPE_TO_LANG_PROPERTY_MAP>;
export type LanguageScope = keyof typeof SCOPE_TO_LANG_PROPERTY_MAP;

export const getScopeLanguagePermission = (
  scope: PermissionModelScope | undefined
): LanguageScope | undefined => {
  if (!scope) {
    return undefined;
  }
  return SCOPE_TO_LANG_PROPERTY_MAP[scope];
};

export const getLanguagesUnion = (
  scopes: PermissionModelScope[],
  permittedLanguages: LanguagePermissions,
  allLangs: number[]
) => {
  const result = new Set<number>();
  let all = false;
  scopes.forEach((scope) => {
    const languageProp = getScopeLanguagePermission(scope);
    if (languageProp) {
      const dependantLanguages =
        languageProp && permittedLanguages[languageProp];
      dependantLanguages?.forEach((langId) => result.add(langId));
      if (
        isAllLanguages(dependantLanguages) ||
        ALL_LANGUAGES_SCOPES.includes(scope)
      ) {
        all = true;
      }
    }
  });
  return all ? [] : result.size ? Array.from(result) : false;
};

export const getLanguagesIntersection = (
  scopes: PermissionModelScope[],
  permittedLanguages: LanguagePermissions,
  allLangs: number[]
) => {
  let result: number[] = [];
  scopes.forEach((scope) => {
    const languageProp = getScopeLanguagePermission(scope);
    const dependantLanguages = languageProp && permittedLanguages[languageProp];

    if (!isSubset(result, dependantLanguages)) {
      if (isAllLanguages(result)) {
        result = allLangs;
      }
      result = result.filter((l) => dependantLanguages.includes(l));
    }
  });
  return Array.from(result);
};

export const isSubset = (subset: number[], wholeSet: number[]) => {
  if (isAllLanguages(wholeSet)) {
    return true;
  } else if (isAllLanguages(subset)) {
    return false;
  }
  return !subset.find((item) => !wholeSet.includes(item));
};

export const isAllLanguages = (langs: number[] | undefined) => {
  return !langs?.length;
};

export const updateByDependencies = (
  myScopes: PermissionModelScope[],
  currentState: PermissionAdvancedState,
  dependencies: HierarchyItem,
  allLangs: number[]
) => {
  const newState = {
    ...currentState,
    scopes: Array.from(new Set([...currentState.scopes, ...myScopes])),
  };
  myScopes.forEach((myScope) => {
    const minimalLanguages = ALL_LANGUAGES_SCOPES.includes(myScope)
      ? []
      : getLanguagesUnion([myScope], newState, allLangs);
    getRequiredScopes(myScope, dependencies).forEach((requiredScope) => {
      if (!newState.scopes.includes(requiredScope)) {
        // add required scope to selected scopes
        newState.scopes = [...newState.scopes, requiredScope];
      }
      const languageProp = getScopeLanguagePermission(requiredScope);
      if (
        minimalLanguages !== false &&
        languageProp &&
        !isSubset(minimalLanguages, newState[languageProp])
      ) {
        if (isAllLanguages(minimalLanguages)) {
          newState[languageProp] = [];
        } else {
          newState[languageProp] = Array.from(
            new Set([...newState[languageProp], ...minimalLanguages])
          );
        }
      }
    });
  });
  return newState;
};

export const updateByDependenciesSoftly = (
  myScopes: PermissionModelScope[],
  currentState: PermissionAdvancedState,
  dependencies: HierarchyItem,
  allLangs: number[]
) => {
  const newState = {
    ...currentState,
    scopes: Array.from(new Set([...currentState.scopes, ...myScopes])),
  };
  myScopes.forEach((myScope) => {
    getRequiredScopes(myScope, dependencies).forEach((requiredScope) => {
      if (!newState.scopes.includes(requiredScope)) {
        // add required scope to selected scopes
        newState.scopes = [...newState.scopes, requiredScope];
      }
      const languageProp = getScopeLanguagePermission(requiredScope);
      if (languageProp) {
        const requiredScopes = getRequiredScopes(requiredScope, dependencies);
        const maximalLanguages = getLanguagesIntersection(
          requiredScopes,
          newState,
          allLangs
        );

        if (!isSubset(newState[languageProp], maximalLanguages)) {
          newState[languageProp] = maximalLanguages;
        }
      }
    });
  });
  return newState;
};
