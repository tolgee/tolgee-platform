import { describe, expect, it } from 'vitest';
import { getProgressData } from './getProgressData';

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
      wordsLimit: 50_000,
      isPayAsYouGo: false,
      ...overrides,
    } as any);

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

  it('shows the word bar on a word plan', () => {
    const { wordsProgress } = getProgressData({ usage: usage() });

    expect(wordsProgress.isInUse).toBe(true);
    expect(wordsProgress.progress).toBe(100 / 50_000);
  });

  it('hides the word bar when the plan does not meter words', () => {
    const { wordsProgress } = getProgressData({
      usage: usage({ includedWords: -1, wordsLimit: -1 }),
    });

    expect(wordsProgress.isInUse).toBe(false);
    expect(wordsProgress.progress).toBe(0);
  });

  it('hides the word bar for an allowance the server does not enforce', () => {
    // A keys-and-seats plan carrying a residual word allowance.
    const { wordsProgress, isCritical } = getProgressData({
      usage: usage({
        includedWords: 50_000,
        currentWords: 60_000,
        wordsLimit: -1,
      }),
    });

    expect(wordsProgress.isInUse).toBe(false);
    expect(isCritical).toBe(false);
  });

  it('does not let an unmetered word count raise the critical warning', () => {
    const { isCritical } = getProgressData({
      usage: usage({ includedWords: -1, currentWords: 100_000 }),
    });

    expect(isCritical).toBe(false);
  });

  it('raises the critical warning once the word allowance runs low', () => {
    const { isCritical } = getProgressData({
      usage: usage({ currentWords: 49_000 }),
    });

    expect(isCritical).toBe(true);
  });
});

describe('getProgressData isExceeded', () => {
  const usage = (overrides = {}) =>
    ({
      isPayAsYouGo: false,
      includedKeys: 1000,
      currentKeys: 0,
      keysLimit: 1000,
      includedTranslations: -1,
      currentTranslations: 0,
      translationsLimit: -1,
      includedSeats: 10,
      currentSeats: 0,
      seatsLimit: 10,
      includedMtCredits: 10000,
      usedMtCredits: 0,
      includedWords: -1,
      currentWords: 0,
      wordsLimit: -1,
      ...overrides,
    } as any);

  it('is false below the limit', () => {
    expect(
      getProgressData({ usage: usage({ currentKeys: 999 }) }).isExceeded
    ).toBe(false);
  });

  it('is true when any metric reaches its limit', () => {
    expect(
      getProgressData({ usage: usage({ currentSeats: 10 }) }).isExceeded
    ).toBe(true);
  });

  it('is false on pay-as-you-go even over the limit', () => {
    expect(
      getProgressData({
        usage: usage({ currentKeys: 2000, isPayAsYouGo: true }),
      }).isExceeded
    ).toBe(false);
  });
});
