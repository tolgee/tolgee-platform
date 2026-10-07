import { hasSupportChat } from '../supportChat';

describe('hasSupportChat', () => {
  it('offers the chat to a member of an entitled organization', () => {
    expect(
      hasSupportChat({
        limitedView: false,
        features: ['STANDARD_SUPPORT'],
      })
    ).toBe(true);
    expect(
      hasSupportChat({
        limitedView: false,
        features: ['PREMIUM_SUPPORT'],
      })
    ).toBe(true);
  });

  it('withholds the chat from a public-project viewer of an entitled organization', () => {
    expect(
      hasSupportChat({
        limitedView: true,
        features: ['PREMIUM_SUPPORT'],
      })
    ).toBe(false);
  });

  it('withholds the chat when the organization is not entitled', () => {
    expect(hasSupportChat({ limitedView: false, features: [] })).toBe(false);
    expect(
      hasSupportChat({ limitedView: undefined, features: ['TASKS'] })
    ).toBe(false);
  });
});
