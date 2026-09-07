import { describe, expect, it } from 'vitest';
import { getSelfHostedProgressData } from './getSelfHostedProgressData';

describe('self-hosted usage progress', () => {
  const usage = (overrides = {}) =>
    ({
      keys: { included: 100, current: 10, limit: 100 },
      seats: { included: 10, current: 2, limit: 10 },
      credits: { included: 1000, current: 100, limit: 1000 },
      ...overrides,
    } as any);

  it('builds a word bar when the licence reports words', () => {
    const { wordsProgress } = getSelfHostedProgressData({
      usage: usage({
        words: { included: 50_000, current: 5_000, limit: 50_000 },
      }),
    });

    expect(wordsProgress?.isInUse).toBe(true);
    expect(wordsProgress?.progress).toBe(5_000 / 50_000);
  });

  it('has no word bar when the licence server predates the words limit', () => {
    const { wordsProgress } = getSelfHostedProgressData({ usage: usage() });

    expect(wordsProgress).toBeUndefined();
  });

  it('hides a bar the licence reports as unenforced', () => {
    const { keysProgress, wordsProgress } = getSelfHostedProgressData({
      usage: usage({
        keys: { included: 100, current: 10, limit: -1 },
        words: { included: 50_000, current: 5_000, limit: -1 },
      }),
    });

    expect(keysProgress.isInUse).toBe(false);
    // Distinct from words being absent entirely: the bar exists, it is just not enforced.
    expect(wordsProgress).toBeDefined();
    expect(wordsProgress?.isInUse).toBe(false);
  });

  it('still builds the other bars without words', () => {
    const { keysProgress, seatsProgress, creditProgress } =
      getSelfHostedProgressData({ usage: usage() });

    expect(keysProgress.isInUse).toBe(true);
    expect(seatsProgress.isInUse).toBe(true);
    expect(creditProgress.isInUse).toBe(true);
  });
});
