import { describe, expect, it } from 'vitest';

import { isExternalValue } from '../externalValueSync';

describe('isExternalValue', () => {
  it('ignores a value the editor emitted while the parent lagged behind', () => {
    // user typed "ab" then "abc"; parent re-renders with the older "ab"
    expect(isExternalValue('ab', 'abc', new Set(['ab', 'abc']))).toBe(false);
  });

  it('applies a value the editor never emitted', () => {
    expect(
      isExternalValue('loaded prompt', 'abc', new Set(['ab', 'abc']))
    ).toBe(true);
  });

  it('does nothing when the parent has caught up', () => {
    expect(isExternalValue('abc', 'abc', new Set(['abc']))).toBe(false);
  });

  it('applies an external value when nothing is in flight', () => {
    expect(isExternalValue('loaded prompt', 'abc', new Set())).toBe(true);
  });

  it('ignores the value held before the first edit', () => {
    // editor started empty, user typed "a", parent still renders the empty
    // initial value; applying it would delete the typed character
    expect(isExternalValue('', 'a', new Set(['', 'a']))).toBe(false);
  });
});
