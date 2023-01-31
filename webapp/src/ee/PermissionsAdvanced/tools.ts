import {
  HierarchyItem,
  HierarchyType,
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

export const getDependent = (
  myScopes: PermissionModelScope[],
  dependencies: HierarchyItem
) => {
  const result = new Set<PermissionModelScope>();

  dependencies.requires.forEach((item) => {
    const childItems = getDependent(myScopes, item);
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
