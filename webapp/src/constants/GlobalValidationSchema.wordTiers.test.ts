import { describe, expect, it } from 'vitest';
import { Validation } from './GlobalValidationSchema';

/**
 * An annual-only plan zeroes every monthly price and stops rendering the monthly column, so a
 * schema demanding a monthly price makes such a plan unsaveable — and silently, since the error
 * attaches to a field that is no longer on screen.
 */
describe('word-tier prices', () => {
  const plan = (tier: Record<string, unknown>) => ({
    name: 'Words',
    type: 'FIXED',
    metricType: 'HOSTED_WORDS',
    free: false,
    stripeProductId: 'prod_x',
    tiers: [{ includedWords: 50000, includedMtCredits: 1000, ...tier }],
  });

  it('accepts a tier priced yearly only', async () => {
    await expect(
      Validation.CLOUD_PLAN_FORM.validate(
        plan({ eurMonthly: 0, eurYearly: 990 })
      )
    ).resolves.toBeTruthy();
  });

  it('accepts a tier priced monthly only', async () => {
    await expect(
      Validation.CLOUD_PLAN_FORM.validate(
        plan({ eurMonthly: 99, eurYearly: 0 })
      )
    ).resolves.toBeTruthy();
  });

  it('rejects a tier with no EUR price at all', async () => {
    await expect(
      Validation.CLOUD_PLAN_FORM.validate(plan({ eurMonthly: 0, eurYearly: 0 }))
    ).rejects.toThrow();
  });

  it('accepts a self-hosted tier priced yearly only', async () => {
    await expect(
      Validation.EE_PLAN_FORM.validate(plan({ eurMonthly: 0, eurYearly: 990 }))
    ).resolves.toBeTruthy();
  });
});
