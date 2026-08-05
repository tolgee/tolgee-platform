import { describe, expect, it } from 'vitest';
import { getProgressData } from './getProgressData';

/**
 * A word plan carries an includedSeats allowance for its free tier, but nothing enforces it —
 * the server reports seatsLimit as unlimited. Rendering a bar for it puts an organization
 * permanently over a limit that does not exist.
 */
describe('usage progress', () => {
  const usage = (overrides = {}) =>
    ({
      includedSeats: 3,
      currentSeats: 8,
      seatsLimit: -1,
      includedTranslations: 0,
      currentTranslations: 0,
      includedKeys: 0,
      currentKeys: 0,
      includedMtCredits: 0,
      usedMtCredits: 0,
      includedWords: 50_000,
      currentWords: 100,
      isPayAsYouGo: false,
      ...overrides,
    }) as any;

  it('hides the seat bar when seats are not enforced', () => {
    const { seatsProgress } = getProgressData({ usage: usage() });

    expect(seatsProgress.isInUse).toBe(false);
  });

  it('does not let an unenforced seat count raise the critical warning', () => {
    // 8 of 3 would otherwise be 266% and pin the top-bar warning on permanently.
    const { isCritical } = getProgressData({ usage: usage() });

    expect(isCritical).toBe(false);
  });

  it('still shows the seat bar when seats are enforced', () => {
    const { seatsProgress } = getProgressData({
      usage: usage({ seatsLimit: 3 }),
    });

    expect(seatsProgress.isInUse).toBe(true);
    expect(seatsProgress.progress).toBeGreaterThan(1);
  });
});
