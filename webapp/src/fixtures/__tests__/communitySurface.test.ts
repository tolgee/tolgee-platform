import { offersCommunityProjects } from 'tg.fixtures/communitySurface';
import { organization } from 'tg.fixtures/__tests__/organizationTestData';

describe('offersCommunityProjects', () => {
  it('offers the list to a contributor whatever organization they are on', () => {
    expect(
      offersCommunityProjects({
        hasCommunityContributions: true,
        organization: organization({ limitedView: false }),
      })
    ).toBe(true);
  });

  it('offers the list while on an organization reached through its public projects', () => {
    expect(
      offersCommunityProjects({
        hasCommunityContributions: false,
        organization: organization({ limitedView: true }),
      })
    ).toBe(true);
  });

  it('does not offer the list to a member with no contributions', () => {
    expect(
      offersCommunityProjects({
        hasCommunityContributions: false,
        organization: organization({ limitedView: false }),
      })
    ).toBe(false);
  });

  it('does not offer the list to a viewer with no organization and no contributions', () => {
    expect(
      offersCommunityProjects({
        hasCommunityContributions: false,
        organization: undefined,
      })
    ).toBe(false);
  });
});
