import { HierarchyType } from 'tg.component/PermissionsSettings/types';

export type ScopeGroup = {
  label?: string;
  scopes: string[];
};

// Maps each scope to the label of its nearest labeled ancestor in the permission hierarchy, so the consent screen can
// render "<resource>: <action> <action>" instead of a flat list of ambiguous action names (View, Edit, View...).
const buildLabelMap = (
  node: HierarchyType,
  ancestorLabel: string | undefined,
  map: Map<string, string | undefined>
) => {
  if (node.value !== undefined) {
    map.set(node.value, ancestorLabel);
  }
  const childAncestor = node.label ?? ancestorLabel;
  node.children?.forEach((child) => buildLabelMap(child, childAncestor, map));
};

export const groupConsentScopes = (
  scopes: string[],
  structure: HierarchyType
): ScopeGroup[] => {
  const labelMap = new Map<string, string | undefined>();
  buildLabelMap(structure, undefined, labelMap);

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
