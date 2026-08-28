import { organizationHasSupportChat } from 'tg.fixtures/supportChat';
import { organization } from 'tg.fixtures/__tests__/organizationTestData';

describe('organizationHasSupportChat', () => {
  it('accepts either support tier', () => {
    expect(
      organizationHasSupportChat(
        organization({ enabledFeatures: ['STANDARD_SUPPORT'] })
      )
    ).toBe(true);
    expect(
      organizationHasSupportChat(
        organization({ enabledFeatures: ['PREMIUM_SUPPORT'] })
      )
    ).toBe(true);
  });

  it('rejects an organization with neither support tier', () => {
    expect(organizationHasSupportChat(organization({}))).toBe(false);
  });

  it('rejects both support tiers on an organization the viewer does not belong to', () => {
    expect(
      organizationHasSupportChat(
        organization({
          limitedView: true,
          currentUserRole: undefined,
          enabledFeatures: ['STANDARD_SUPPORT'],
        })
      )
    ).toBe(false);
    expect(
      organizationHasSupportChat(
        organization({
          limitedView: true,
          currentUserRole: undefined,
          enabledFeatures: ['PREMIUM_SUPPORT'],
        })
      )
    ).toBe(false);
  });

  it('rejects a viewer without any organization', () => {
    expect(organizationHasSupportChat(undefined)).toBe(false);
  });
});
