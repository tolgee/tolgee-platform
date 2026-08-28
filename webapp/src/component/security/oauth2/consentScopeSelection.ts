export const clampApprovedScopes = (
  changed: string[],
  requestedScopes: string[],
  requiredScopes: string[]
): string[] => {
  const next = new Set(changed);
  requiredScopes.forEach((s) => next.add(s));
  return requestedScopes.filter((s) => next.has(s));
};
