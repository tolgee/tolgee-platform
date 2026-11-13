import { HierarchyType } from 'tg.component/PermissionsSettings/types';

type LabeledScopes = {
  /** Absent for scopes that stand on their own in the permission tree. */
  label?: string;
  scopes: string[];
};

export type ScopeGroup = LabeledScopes & {
  /** Every scope the resource has was granted - not merely every scope the client asked for. */
  complete: boolean;
};

/**
 * The tree's root scope grants every other one, so when it is granted it is returned on its own.
 */
export const groupConsentScopes = (
  scopes: string[],
  structure: HierarchyType
): ScopeGroup[] => {
  const rootScope = structure.value;
  if (rootScope !== undefined && scopes.includes(rootScope)) {
    return [{ label: undefined, scopes: [rootScope], complete: true }];
  }

  const labelMap = scopeLabels(structure);
  const order = treeOrder(labelMap);
  const scopesPerLabel = countScopesPerLabel(labelMap);
  const withCompleteness: ScopeGroup[] = groupByLabel(scopes, labelMap).map(
    (group) => ({
      ...group,
      scopes: sortedByTreeOrder(group.scopes, order),
      complete: group.scopes.length === scopesPerLabel.get(group.label),
    })
  );
  return unlabeledFirst(withCompleteness);
};

export const showsAsOneChip = (group: ScopeGroup): boolean =>
  Boolean(group.label) && group.complete && group.scopes.length > 1;

const groupByLabel = (
  scopes: string[],
  labelMap: Map<string, string | undefined>
): LabeledScopes[] => {
  const groups: LabeledScopes[] = [];
  const byLabel = new Map<string | undefined, LabeledScopes>();
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

const countScopesPerLabel = (
  labelMap: Map<string, string | undefined>
): Map<string | undefined, number> => {
  const counts = new Map<string | undefined, number>();
  labelMap.forEach((label) => counts.set(label, (counts.get(label) ?? 0) + 1));
  return counts;
};

const treeOrder = (
  labelMap: Map<string, string | undefined>
): Map<string, number> =>
  new Map([...labelMap.keys()].map((scope, i) => [scope, i]));

const sortedByTreeOrder = (
  scopes: string[],
  order: Map<string, number>
): string[] => {
  const orderOf = (scope: string) =>
    order.get(scope) ?? Number.MAX_SAFE_INTEGER;
  return [...scopes].sort((a, b) => orderOf(a) - orderOf(b));
};

const unlabeledFirst = (groups: ScopeGroup[]): ScopeGroup[] => [
  ...groups.filter((group) => !group.label),
  ...groups.filter((group) => group.label),
];

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
