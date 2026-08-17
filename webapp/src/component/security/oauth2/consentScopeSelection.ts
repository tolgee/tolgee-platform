// The consent form POSTs exactly these scopes and the backend issues them verbatim, so this clamp is the sole guard on
// consent integrity: the approved set can never grant beyond what the app requested, and a required scope can never be
// dropped (e.g. by toggling a parent group off).
export const clampApprovedScopes = (
  changed: string[],
  requestedScopes: string[],
  requiredScopes: string[]
): string[] => {
  const next = new Set(changed);
  requiredScopes.forEach((s) => next.add(s));
  return requestedScopes.filter((s) => next.has(s));
};
