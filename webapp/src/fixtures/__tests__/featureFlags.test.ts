import { hasFeatureFlag } from 'tg.fixtures/featureFlags';

describe('hasFeatureFlag', () => {
  it('is enabled when the organization has the flag', () => {
    expect(hasFeatureFlag(['AI_REVIEW'], 'AI_REVIEW')).toBe(true);
  });

  it('is disabled when the organization does not have the flag', () => {
    expect(hasFeatureFlag([], 'AI_REVIEW')).toBe(false);
  });

  it('is disabled while the organization is not loaded', () => {
    expect(hasFeatureFlag(undefined, 'AI_REVIEW')).toBe(false);
  });
});
