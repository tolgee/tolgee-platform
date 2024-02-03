import { HOST } from '../../common/constants';
import 'cypress-file-upload';
import { gcy } from '../../common/shared';
import { login, setBypassSeatCountCheck } from '../../common/apiCalls/common';
import { organizationTestData } from '../../common/apiCalls/testData/testData';
import {
  permissionsMenuSelectAdvanced,
  permissionsMenuSelectRole,
} from '../../common/permissionsMenu';

describe('Organization Base permissions', () => {
  let organizationData: Record<string, { slug: string }>;

  beforeEach(() => {
    setBypassSeatCountCheck(true);
    login();
    organizationTestData.clean();
    organizationTestData.generate().then((res) => {
      organizationData = res.body as any;
      visitProfile('Tolgee');
    });
  });

  afterEach(() => {
    setBypassSeatCountCheck(false);
  });

  it('changes member privileges', () => {
    gcy('organization-side-menu').contains('Member permissions').click();
    gcy('permissions-menu-button').click();
    permissionsMenuSelectRole('Translate', { confirm: true });
    visitMemberPrivileges('Tolgee');
    gcy('permissions-menu-button').contains('Translate');
  });

  it("member privileges change doesn't affect profile", () => {
    gcy('organization-side-menu').contains('Member permissions').click();
    gcy('permissions-menu-button').click();
    permissionsMenuSelectRole('Translate', { confirm: true });
    visitProfile('Tolgee');
    gcy('organization-name-field').within(() =>
      cy.get('input').should('have.value', 'Tolgee')
    );
    gcy('organization-address-part-field').within(() =>
      cy.get('input').should('contain.value', 'tolgee')
    );
    gcy('organization-description-field').within(() =>
      cy.get('input').should('have.value', 'This is us')
    );
  });

  it('changes advanced permissions', () => {
    gcy('organization-side-menu').contains('Member permissions').click();
    gcy('permissions-menu-button').click();
    permissionsMenuSelectAdvanced(['keys.create', 'keys.edit'], {
      confirm: true,
    });
  });

  after(() => {
    organizationTestData.clean();
  });

  const visitProfile = (name: string) => {
    visitPath(name, `/profile`);
  };

  const visitMemberPrivileges = (name: string) => {
    visitPath(name, `/member-privileges`);
  };

  function visitSlug(slug: string, path: string) {
    cy.visit(`${HOST}/organizations/${slug}${path}`);
  }

  const visitPath = (name: string, path: string) => {
    const slug = organizationData[name].slug;
    visitSlug(slug, path);
  };
});
