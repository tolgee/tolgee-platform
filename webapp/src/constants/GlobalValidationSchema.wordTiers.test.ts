import { describe, expect, it } from 'vitest';
import { Validation } from './GlobalValidationSchema';

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

  it('reports the missing price on the yearly field when nothing is priced', async () => {
    // With annual billing only the monthly column is not rendered, so an error attached solely
    // to the monthly field would be invisible and Save would fail silently.
    const error = await Validation.CLOUD_PLAN_FORM.validate(
      plan({ eurMonthly: 0, eurYearly: 0 }),
      { abortEarly: false }
    ).catch((e) => e);

    expect(error.inner.map((i: any) => i.path)).toContain('tiers[0].eurYearly');
  });

  // Both forms share one tiers schema, so each branch is pinned from both entry points.
  it('accepts a self-hosted tier priced yearly only', async () => {
    await expect(
      Validation.EE_PLAN_FORM.validate(plan({ eurMonthly: 0, eurYearly: 990 }))
    ).resolves.toBeTruthy();
  });

  it('rejects a self-hosted tier with no EUR price at all', async () => {
    await expect(
      Validation.EE_PLAN_FORM.validate(plan({ eurMonthly: 0, eurYearly: 0 }))
    ).rejects.toThrow();
  });

  it('accepts a free self-hosted tier with no price at all', async () => {
    await expect(
      Validation.EE_PLAN_FORM.validate({
        ...plan({ eurMonthly: 0, eurYearly: 0 }),
        free: true,
      })
    ).resolves.toBeTruthy();
  });

  it('applies the default tier schema to a self-hosted keys-and-seats plan', async () => {
    await expect(
      Validation.EE_PLAN_FORM.validate({
        ...plan({}),
        metricType: 'KEYS_SEATS',
        tiers: [{ includedKeys: 1000, includedSeats: 5, eurMonthly: 20 }],
      })
    ).resolves.toBeTruthy();
  });

  it('accepts a free tier with no price at all', async () => {
    await expect(
      Validation.CLOUD_PLAN_FORM.validate({
        ...plan({ eurMonthly: 0, eurYearly: 0 }),
        free: true,
      })
    ).resolves.toBeTruthy();
  });

  it('still requires the word allowance on a free tier, as the server does', async () => {
    await expect(
      Validation.CLOUD_PLAN_FORM.validate({
        ...plan({ eurMonthly: 0, eurYearly: 0 }),
        free: true,
        tiers: [{ includedMtCredits: 1000 }],
      })
    ).rejects.toThrow();
  });
});
