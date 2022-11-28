import { HOST } from '../../common/constants';
import 'cypress-file-upload';
import { gcy } from '../../common/shared';
import { login } from '../../common/apiCalls/common';
import { organizationTestData } from '../../common/apiCalls/testData/testData';
import {
  permissionsMenuSelectAdvanced,
  permissionsMenuSelectRole,
} from '../../common/permissionsMenu';

describe('Organization Base permissions', () => {
  beforeEach(() => {
    login();
    organizationTestData.clean();
    organizationTestData.generate();
    visitProfile();
  });

  it('changes member privileges', () => {
    gcy('organization-side-menu').contains('Member permissions').click();
    gcy('permissions-menu-button').click();
    permissionsMenuSelectRole('Translate', { confirm: true });
    visitMemberPrivileges();
    gcy('permissions-menu-button').contains('Translate');
  });

  it("member privileges change doesn't affect profile", () => {
    gcy('organization-side-menu').contains('Member permissions').click();
    gcy('permissions-menu-button').click();
    permissionsMenuSelectRole('Translate', { confirm: true });
    visitProfile();
    gcy('organization-name-field').within(() =>
      cy.get('input').should('have.value', 'Tolgee')
    );
    gcy('organization-address-part-field').within(() =>
      cy.get('input').should('have.value', 'tolgee')
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

  const visitProfile = () => {
    cy.visit(`${HOST}/organizations/tolgee/profile`);
  };

  const visitMemberPrivileges = () => {
    cy.visit(`${HOST}/organizations/tolgee/member-privileges`);
  };
});
