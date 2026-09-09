import { components } from 'tg.service/apiSchema.generated';

import { getProgressData } from './getProgressData';

type UsageModel = components['schemas']['PublicUsageModel'];

const usage = (overrides: Partial<UsageModel>): UsageModel =>
  ({
    isPayAsYouGo: false,
    includedKeys: 1000,
    currentKeys: 0,
    includedTranslations: -1,
    currentTranslations: 0,
    includedSeats: 10,
    currentSeats: 0,
    includedMtCredits: 10000,
    usedMtCredits: 0,
    ...overrides,
  } as UsageModel);

describe('getProgressData isExceeded', () => {
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
