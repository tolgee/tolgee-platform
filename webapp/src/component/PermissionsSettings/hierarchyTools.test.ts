import { describe, expect, it } from 'vitest';

import { isNodeDisabled } from './hierarchyTools';
import { PermissionModelScope } from './types';

const scopes = (...values: string[]) => values as PermissionModelScope[];

describe('isNodeDisabled', () => {
  it('disables a node whose scopes are all locked by the application', () => {
    expect(
      isNodeDisabled({
        myScopes: scopes('keys.view'),
        lockedScopes: scopes('keys.view'),
        blockingScopes: scopes(),
      })
    ).toBe(true);
  });

  it('leaves an optional scope toggleable when only some of the tree is locked', () => {
    expect(
      isNodeDisabled({
        myScopes: scopes('keys.edit'),
        lockedScopes: scopes('keys.view'),
        blockingScopes: scopes(),
      })
    ).toBe(false);
  });

  it('still blocks a scope another checked scope depends on when a locked set is supplied', () => {
    // The regression this guards: an empty lockedScopes array is truthy, and forking on it switched dependency
    // blocking off for the whole tree — letting the consent screen uncheck keys.view while keys.edit stayed checked.
    expect(
      isNodeDisabled({
        myScopes: scopes('keys.view'),
        lockedScopes: scopes(),
        blockingScopes: scopes('keys.edit'),
      })
    ).toBe(true);
  });

  it('blocks on dependencies when no locked set is supplied at all', () => {
    expect(
      isNodeDisabled({
        myScopes: scopes('keys.view'),
        lockedScopes: undefined,
        blockingScopes: scopes('keys.edit'),
      })
    ).toBe(true);
  });

  it('leaves a node alone when neither reason applies', () => {
    expect(
      isNodeDisabled({
        myScopes: scopes('keys.view'),
        lockedScopes: undefined,
        blockingScopes: scopes(),
      })
    ).toBe(false);
  });

  it('does not lock a node that owns no scopes', () => {
    expect(
      isNodeDisabled({
        myScopes: scopes(),
        lockedScopes: scopes(),
        blockingScopes: scopes(),
      })
    ).toBe(false);
  });
});
