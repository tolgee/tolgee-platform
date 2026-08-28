import { organizationSwitchShape } from 'tg.fixtures/organizationSwitchShape';

const shape = (over: Partial<Parameters<typeof organizationSwitchShape>[0]>) =>
  organizationSwitchShape({
    hasPreferredOrganization: false,
    isCommunitySurface: false,
    canCreateOrganization: false,
    showsCommunityProjects: false,
    ...over,
  });

describe('organizationSwitchShape', () => {
  it('names the preferred organization on an organization surface', () => {
    expect(shape({ hasPreferredOrganization: true })).toEqual({
      render: 'switch',
      label: 'organization',
      offersCommunityFooter: false,
      communitySelected: false,
    });
  });

  it('hides itself entirely for a viewer with no organization outside the community surface', () => {
    expect(shape({}).render).toBe('hidden');
  });

  it('offers the community footer from an organization, not from the community surface', () => {
    expect(
      shape({ hasPreferredOrganization: true, showsCommunityProjects: true })
    ).toEqual({
      render: 'switch',
      label: 'organization',
      offersCommunityFooter: true,
      communitySelected: false,
    });
  });

  it('offers the switch when the only thing to do is create an organization', () => {
    expect(
      shape({ isCommunitySurface: true, canCreateOrganization: true }).render
    ).toBe('switch');
  });

  it('falls back to an inert label when the popover would have nothing to offer', () => {
    expect(shape({ isCommunitySurface: true })).toEqual({
      render: 'community-label',
      offersCommunityFooter: false,
      communitySelected: true,
    });
  });

  it('stays inert for an org-less viewer already on the community surface', () => {
    // Its only action would navigate to the page they are already on, so there is no popover worth
    // opening — the same outcome as having no community projects at all.
    expect(
      shape({
        hasPreferredOrganization: false,
        isCommunitySurface: true,
        canCreateOrganization: false,
        showsCommunityProjects: true,
      })
    ).toEqual({
      render: 'community-label',
      offersCommunityFooter: false,
      communitySelected: true,
    });
  });

  it('keeps the community label on the community surface even with an organization', () => {
    expect(
      shape({ hasPreferredOrganization: true, isCommunitySurface: true })
    ).toEqual({
      render: 'switch',
      label: 'community',
      offersCommunityFooter: false,
      communitySelected: true,
    });
  });
});
