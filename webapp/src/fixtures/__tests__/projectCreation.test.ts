import { OrganizationRole } from 'tg.fixtures/organizationRole';
import {
  canCreateProject,
  projectCreationRefusal,
} from 'tg.fixtures/projectCreation';
import { organization as buildOrganization } from 'tg.fixtures/__tests__/organizationTestData';

const organization = (currentUserRole: OrganizationRole) =>
  buildOrganization({ currentUserRole });

describe('canCreateProject', () => {
  it('lets a server admin create in an organization they have no role in', () => {
    expect(
      canCreateProject({ isAdmin: true, organization: organization(undefined) })
    ).toBe(true);
  });

  it('refuses a server admin with no organization at all', () => {
    expect(canCreateProject({ isAdmin: true, organization: undefined })).toBe(
      false
    );
  });

  it('lets an owner or a maintainer create', () => {
    expect(
      canCreateProject({ isAdmin: false, organization: organization('OWNER') })
    ).toBe(true);
    expect(
      canCreateProject({
        isAdmin: false,
        organization: organization('MAINTAINER'),
      })
    ).toBe(true);
  });

  it('refuses a plain member', () => {
    expect(
      canCreateProject({ isAdmin: false, organization: organization('MEMBER') })
    ).toBe(false);
  });

  it('refuses a viewer with no role in the organization', () => {
    expect(
      canCreateProject({
        isAdmin: false,
        organization: organization(undefined),
      })
    ).toBe(false);
  });
});

describe('projectCreationRefusal', () => {
  it('names the missing organization', () => {
    expect(
      projectCreationRefusal({ isAdmin: false, organization: undefined })
    ).toBe('no-organization');
  });

  it('names the role when the viewer has an organization they may not create in', () => {
    expect(
      projectCreationRefusal({
        isAdmin: false,
        organization: organization('MEMBER'),
      })
    ).toBe('not-owner-or-maintainer');
  });

  it('refuses nothing to a maintainer or an admin', () => {
    expect(
      projectCreationRefusal({
        isAdmin: false,
        organization: organization('MAINTAINER'),
      })
    ).toBeUndefined();
    expect(
      projectCreationRefusal({
        isAdmin: true,
        organization: organization(undefined),
      })
    ).toBeUndefined();
  });
});
