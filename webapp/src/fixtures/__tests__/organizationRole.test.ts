import { isAtLeastMemberOrgRole } from '../organizationRole';

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
