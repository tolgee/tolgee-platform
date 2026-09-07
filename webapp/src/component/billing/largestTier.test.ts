import { describe, expect, it } from 'vitest';
import { isLargestTier } from './largestTier';

describe('isLargestTier', () => {
  const ladder = [
    { id: 1, includedWords: 50_000 },
    { id: 2, includedWords: 100_000 },
    { id: 3, includedWords: 200_000 },
  ];

  it('is true on the biggest tier of the ladder', () => {
    expect(isLargestTier(ladder, 3)).toBe(true);
  });

  it('is false anywhere below it', () => {
    expect(isLargestTier(ladder, 1)).toBe(false);
    expect(isLargestTier(ladder, 2)).toBe(false);
  });

  it('is true on a single-tier offering, which has nothing bigger either', () => {
    expect(isLargestTier([{ id: 7, includedWords: 5_000 }], 7)).toBe(true);
  });

  it('is true when tiers tie at the top', () => {
    const tied = [
      { id: 1, includedWords: 100_000 },
      { id: 2, includedWords: 100_000 },
    ];
    expect(isLargestTier(tied, 1)).toBe(true);
  });

  it('is false when the ladder has not loaded or the tier is unknown', () => {
    expect(isLargestTier(undefined, 3)).toBe(false);
    expect(isLargestTier([], 3)).toBe(false);
    expect(isLargestTier(ladder, undefined)).toBe(false);
    expect(isLargestTier(ladder, null)).toBe(false);
    expect(isLargestTier(ladder, 999)).toBe(false);
  });
});
