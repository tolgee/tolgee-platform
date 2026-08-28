import {
  canCreateOrganization,
  organizationCreationRefusal,
} from 'tg.fixtures/organizationCreation';

describe('canCreateOrganization', () => {
  it('lets an admin create even through SSO on a server that forbids it', () => {
    expect(
      canCreateOrganization({
        isAdmin: true,
        thirdPartyAuthType: 'SSO',
        userCanCreateOrganizations: false,
      })
    ).toBe(true);
  });

  it('refuses a non-admin SSO account even where the server allows creation', () => {
    expect(
      canCreateOrganization({
        isAdmin: false,
        thirdPartyAuthType: 'SSO',
        userCanCreateOrganizations: true,
      })
    ).toBe(false);
  });

  it('lets a server-wide SSO account create, since only organization-wide SSO restricts it', () => {
    expect(
      canCreateOrganization({
        isAdmin: false,
        thirdPartyAuthType: 'SSO_GLOBAL',
        userCanCreateOrganizations: true,
      })
    ).toBe(true);
  });

  it('does not refuse other third party accounts', () => {
    expect(
      canCreateOrganization({
        isAdmin: false,
        thirdPartyAuthType: 'GOOGLE',
        userCanCreateOrganizations: true,
      })
    ).toBe(true);
  });

  it('follows the server property for a plain account', () => {
    expect(
      canCreateOrganization({
        isAdmin: false,
        thirdPartyAuthType: undefined,
        userCanCreateOrganizations: true,
      })
    ).toBe(true);
    expect(
      canCreateOrganization({
        isAdmin: false,
        thirdPartyAuthType: undefined,
        userCanCreateOrganizations: false,
      })
    ).toBe(false);
  });
});

describe('organizationCreationRefusal', () => {
  it('reports the server property for a plain account', () => {
    expect(
      organizationCreationRefusal({
        isAdmin: false,
        thirdPartyAuthType: undefined,
        userCanCreateOrganizations: false,
      })
    ).toBe('server-disallows');
  });

  it('reports SSO where the server would otherwise allow creation', () => {
    expect(
      organizationCreationRefusal({
        isAdmin: false,
        thirdPartyAuthType: 'SSO',
        userCanCreateOrganizations: true,
      })
    ).toBe('sso');
  });

  it('reports the server property before SSO when both apply', () => {
    expect(
      organizationCreationRefusal({
        isAdmin: false,
        thirdPartyAuthType: 'SSO',
        userCanCreateOrganizations: false,
      })
    ).toBe('server-disallows');
  });

  it('reports nothing for a viewer who may create', () => {
    expect(
      organizationCreationRefusal({
        isAdmin: false,
        thirdPartyAuthType: undefined,
        userCanCreateOrganizations: true,
      })
    ).toBeUndefined();
    expect(
      organizationCreationRefusal({
        isAdmin: true,
        thirdPartyAuthType: 'SSO',
        userCanCreateOrganizations: false,
      })
    ).toBeUndefined();
  });
});
