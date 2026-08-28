type Params = {
  hasPreferredOrganization: boolean;
  isCommunitySurface: boolean;
  canCreateOrganization: boolean;
  showsCommunityProjects: boolean;
};

type OrganizationSwitchShape = {
  offersCommunityFooter: boolean;
  communitySelected: boolean;
} & (
  | { render: 'hidden' }
  | { render: 'community-label' }
  | { render: 'switch'; label: 'community' | 'organization' }
);

export const organizationSwitchShape = ({
  hasPreferredOrganization,
  isCommunitySurface,
  canCreateOrganization,
  showsCommunityProjects,
}: Params): OrganizationSwitchShape => {
  const popover = {
    // Not offered on the community surface itself: the footer's destination is the current page.
    offersCommunityFooter: showsCommunityProjects && !isCommunitySurface,
    communitySelected: isCommunitySurface,
  };

  if (!hasPreferredOrganization && !isCommunitySurface) {
    return { ...popover, render: 'hidden' };
  }

  // Only the community surface reaches here without an organization, and its footer would navigate
  // to the current page — so creating one is the only thing a popover could offer.
  if (!hasPreferredOrganization && !canCreateOrganization) {
    return { ...popover, render: 'community-label' };
  }

  return {
    ...popover,
    render: 'switch',
    label:
      isCommunitySurface || !hasPreferredOrganization
        ? 'community'
        : 'organization',
  };
};
