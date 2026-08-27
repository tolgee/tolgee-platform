import { describe, expect, it } from 'vitest';
import {
  isWordsAutoUpgradeAvailable,
  isWordsAutoUpgradeIneffective,
  wordsAutoUpgradeIneffectiveReason,
} from './useWordsAutoUpgrade';

describe('words auto-upgrade offer', () => {
  const subscription = (overrides = {}) =>
    ({
      plan: { metricType: 'HOSTED_WORDS', free: false },
      autoUpgradeEnabled: false,
      ...overrides,
    } as any);

  it('is offered on a paid word plan whose words are exhausted', () => {
    expect(isWordsAutoUpgradeAvailable(subscription(), true)).toBe(true);
  });

  it('is not offered when the limit that was hit is not the word limit', () => {
    // The popover opens for MT-credit limits too, on the same plan.
    expect(isWordsAutoUpgradeAvailable(subscription(), false)).toBe(false);
  });

  it('is not offered on a plan that does not meter words', () => {
    const keysPlan = subscription({
      plan: { metricType: 'KEYS_SEATS', free: false },
    });

    expect(isWordsAutoUpgradeAvailable(keysPlan, true)).toBe(false);
  });

  it('is not offered on a free plan, which has nothing to upgrade from', () => {
    const freePlan = subscription({
      plan: { metricType: 'HOSTED_WORDS', free: true },
    });

    expect(isWordsAutoUpgradeAvailable(freePlan, true)).toBe(false);
  });

  it('is not offered when auto-upgrade is already on', () => {
    expect(
      isWordsAutoUpgradeAvailable(
        subscription({ autoUpgradeEnabled: true }),
        true
      )
    ).toBe(false);
  });

  it('is not offered before the subscription has loaded', () => {
    expect(isWordsAutoUpgradeAvailable(undefined, true)).toBe(false);
  });

  describe('when auto-upgrade is already on', () => {
    const enabled = subscription({ autoUpgradeEnabled: true });

    it('says so instead of offering the toggle', () => {
      // The server only blocks with auto-upgrade on when it cannot apply, so the toggle is a no-op.
      expect(isWordsAutoUpgradeIneffective(enabled, true)).toBe(true);
      expect(isWordsAutoUpgradeAvailable(enabled, true)).toBe(false);
    });

    it('says nothing when the limit that was hit is not the word limit', () => {
      expect(isWordsAutoUpgradeIneffective(enabled, false)).toBe(false);
    });

    it('says nothing on a plan that does not meter words', () => {
      const keysPlan = subscription({
        plan: { metricType: 'KEYS_SEATS', free: false },
        autoUpgradeEnabled: true,
      });

      expect(isWordsAutoUpgradeIneffective(keysPlan, true)).toBe(false);
    });

    it('says nothing before the subscription has loaded', () => {
      expect(isWordsAutoUpgradeIneffective(undefined, true)).toBe(false);
    });

    describe('the reason it cannot be applied', () => {
      const ladder = [
        { id: 1, includedWords: 50_000 },
        { id: 2, includedWords: 200_000 },
      ];
      const onTier = (currentTierId: number, extra = {}) =>
        subscription({
          autoUpgradeEnabled: true,
          plan: {
            metricType: 'HOSTED_WORDS',
            free: false,
            tiers: ladder,
            currentTierId,
          },
          ...extra,
        });

      it('is the largest tier when there is nothing bigger to move to', () => {
        expect(wordsAutoUpgradeIneffectiveReason(onTier(2))).toBe(
          'largestTier'
        );
      });

      it('is the scheduled change when one is pending', () => {
        const withDowngrade = onTier(1, {
          scheduledDowngrade: { name: 'Translate' },
        });
        expect(wordsAutoUpgradeIneffectiveReason(withDowngrade)).toBe(
          'scheduledChange'
        );
      });

      it('prefers the scheduled change, which is the one the customer can undo', () => {
        const both = onTier(2, {
          scheduledDowngrade: { name: 'Translate' },
        });
        expect(wordsAutoUpgradeIneffectiveReason(both)).toBe(
          'scheduledChange'
        );
      });

      it('falls back to other when neither applies', () => {
        // e.g. cancelling at period end, or a non-active status
        expect(wordsAutoUpgradeIneffectiveReason(onTier(1))).toBe('other');
      });

      it('falls back to other before the subscription has loaded', () => {
        expect(wordsAutoUpgradeIneffectiveReason(undefined)).toBe('other');
      });
    });
  });

  it('offers exactly one of the two messages, never both', () => {
    [false, true].forEach((autoUpgradeEnabled) => {
      const sub = subscription({ autoUpgradeEnabled });
      expect(
        [
          isWordsAutoUpgradeAvailable(sub, true),
          isWordsAutoUpgradeIneffective(sub, true),
        ].filter(Boolean)
      ).toHaveLength(1);
    });
  });
});
