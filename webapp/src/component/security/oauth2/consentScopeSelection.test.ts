import { clampApprovedScopes } from './consentScopeSelection';

const REQUESTED = ['keys.view', 'translations.view', 'translations.edit'];
const REQUIRED = ['keys.view'];

describe('clampApprovedScopes', () => {
  it('keeps a required scope even when the change dropped it', () => {
    const result = clampApprovedScopes(
      ['translations.view'],
      REQUESTED,
      REQUIRED
    );
    expect(result).toContain('keys.view');
    expect(result).toContain('translations.view');
  });

  it('removes an optional scope the user unchecked', () => {
    const result = clampApprovedScopes(
      ['keys.view', 'translations.view'],
      REQUESTED,
      REQUIRED
    );
    expect(result).not.toContain('translations.edit');
  });

  it('never adds a scope outside the app-requested set', () => {
    const result = clampApprovedScopes(
      ['keys.view', 'admin', 'members.view'],
      REQUESTED,
      REQUIRED
    );
    expect(result).toEqual(['keys.view']);
  });

  it('returns only requested scopes, preserving their order', () => {
    const result = clampApprovedScopes(
      ['translations.edit', 'translations.view', 'keys.view'],
      REQUESTED,
      REQUIRED
    );
    expect(result).toEqual(REQUESTED);
  });
});
