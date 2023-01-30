import { HierarchyType, PermissionModelScope } from '../types';

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
