import { describe, expect, it } from 'vitest';
import { isMenuItemSelected } from 'tg.fixtures/menuSelection';

describe('settings menu selection', () => {
  it('selects an exact match', () => {
    expect(isMenuItemSelected('/billing', '/billing')).toBe(true);
  });

  it('does not select a different path', () => {
    expect(isMenuItemSelected('/billing/plans', '/billing')).toBe(false);
  });

  it('selects a sub-path when matching as prefix', () => {
    expect(isMenuItemSelected('/billing/plans', '/billing', true)).toBe(true);
  });

  it('does not select a sibling path that merely shares the prefix', () => {
    expect(
      isMenuItemSelected('/billing-test-clock-helper', '/billing', true)
    ).toBe(false);
  });
});
