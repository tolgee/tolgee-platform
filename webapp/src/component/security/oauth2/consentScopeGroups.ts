import { HierarchyType } from 'tg.component/PermissionsSettings/types';

export type ScopeGroup = {
  label?: string;
  scopes: string[];
};

export const groupConsentScopes = (
  scopes: string[],
  structure: HierarchyType
): ScopeGroup[] => {
  const labelMap = scopeLabels(structure);

  const groups: ScopeGroup[] = [];
  const byLabel = new Map<string | undefined, ScopeGroup>();
  scopes.forEach((scope) => {
    const label = labelMap.get(scope);
    let group = byLabel.get(label);
    if (!group) {
      group = { label, scopes: [] };
      byLabel.set(label, group);
      groups.push(group);
    }
    group.scopes.push(scope);
  });
  return groups;
};

const scopeLabels = (
  structure: HierarchyType
): Map<string, string | undefined> => {
  const labels = new Map<string, string | undefined>();
  const collect = (node: HierarchyType, ancestorLabel: string | undefined) => {
    if (node.value !== undefined) {
      labels.set(node.value, ancestorLabel);
    }
    const childAncestor = node.label ?? ancestorLabel;
    node.children?.forEach((child) => collect(child, childAncestor));
  };
  collect(structure, undefined);
  return labels;
};
