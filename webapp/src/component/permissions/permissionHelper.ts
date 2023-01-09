import scopeHierarchy from 'tg.constants/scopes.generated.json';
import { components } from 'tg.service/apiSchema.generated';

export type Scope = components['schemas']['PermissionModel']['scopes'][0];
export type HierarchyItem = { scope: Scope; requires?: HierarchyItem[] };

/**
 * Returns the scopes that are required to be granted for the given scope to be granted.
 */
export function expandScope(scope: Scope): Scope[] {
  const hierarchyItems = getScopeHierarchyItems(
    scopeHierarchy as HierarchyItem,
    scope
  );
  return hierarchyItems.flatMap(expandHierarchyItem) as Scope[];
}

/**
 * Returns the scopes that are required to be granted for the given scope to be granted.
 */
export function getRequirements(scope: Scope): Scope[] {
  const hierarchyItems = getScopeHierarchyItems(
    scopeHierarchy as HierarchyItem,
    scope
  );
  const all = hierarchyItems.flatMap(expandHierarchyItem) as Scope[];
  return all.filter((s) => s !== scope);
}

function expandHierarchyItem(item: HierarchyItem): Scope[] {
  const descendants = item.requires?.flatMap(expandHierarchyItem) ?? [];
  descendants.push(item.scope);
  return descendants;
}

function getScopeHierarchyItems(
  root: HierarchyItem,
  scope: Scope
): HierarchyItem[] {
  const items: HierarchyItem[] = [];
  if (root.scope === scope) {
    items.push(root);
  }
  root.requires?.forEach((child) => {
    items.push(...getScopeHierarchyItems(child, scope));
  });
  return items;
}

/**
 * Returns all scopes recursively
 *
 * Example: When permittedScopes == [ADMIN], it will return [ADMIN, TRANSLATIONS_EDIT, TRANSLATIONS_VIEW, KEYS_EDIT, ...]
 */
export function expand(permittedScopes: Scope[]): Scope[] {
  return [...new Set(permittedScopes.map(expandScope).flat())];
}
