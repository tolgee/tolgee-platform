import {
  canManageOrganization,
  isAtLeastMemberOrgRole,
  isOwnerOrMaintainerOrgRole,
} from 'tg.fixtures/organizationRole';

describe('isAtLeastMemberOrgRole', () => {
  it('treats MEMBER and above as members', () => {
    expect(isAtLeastMemberOrgRole('MEMBER')).toBe(true);
    expect(isAtLeastMemberOrgRole('MAINTAINER')).toBe(true);
    expect(isAtLeastMemberOrgRole('OWNER')).toBe(true);
  });

  it('does not treat a missing role as a member', () => {
    expect(isAtLeastMemberOrgRole(undefined)).toBe(false);
    expect(isAtLeastMemberOrgRole(null)).toBe(false);
  });
});

describe('isOwnerOrMaintainerOrgRole', () => {
  it('accepts OWNER and MAINTAINER', () => {
    expect(isOwnerOrMaintainerOrgRole('OWNER')).toBe(true);
    expect(isOwnerOrMaintainerOrgRole('MAINTAINER')).toBe(true);
  });

  it('rejects MEMBER', () => {
    expect(isOwnerOrMaintainerOrgRole('MEMBER')).toBe(false);
  });

  it('rejects a missing role', () => {
    expect(isOwnerOrMaintainerOrgRole(undefined)).toBe(false);
    expect(isOwnerOrMaintainerOrgRole(null)).toBe(false);
  });
});

describe('canManageOrganization', () => {
  it('admits an owner of the organization', () => {
    expect(canManageOrganization('OWNER', false)).toBe(true);
  });

  it('admits staff who hold no role in it', () => {
    expect(canManageOrganization(undefined, true)).toBe(true);
  });

  it('refuses a plain member', () => {
    expect(canManageOrganization('MEMBER', false)).toBe(false);
  });

  it('refuses a maintainer, who cannot reach owner-only settings', () => {
    expect(canManageOrganization('MAINTAINER', false)).toBe(false);
  });
});
