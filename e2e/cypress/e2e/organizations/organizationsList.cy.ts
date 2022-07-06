import { HOST } from '../../common/constants';
import 'cypress-file-upload';
import { clickGlobalSave, gcy, goToPage } from '../../common/shared';
import { organizationTestData } from '../../common/apiCalls/testData/testData';
import { login } from '../../common/apiCalls/common';

describe('Organization List', () => {
  beforeEach(() => {
    login();
    organizationTestData.clean();
    organizationTestData.generate();
    visit();
  });

  it('creates organization', () => {
    gcy('global-plus-button').click();
    gcy('organization-name-field').within(() =>
      cy.get('input').type('What a nice organization')
    );
    gcy('organization-address-part-field').within(() =>
      cy.get('input').should('have.value', 'what-a-nice-organization')
    );
    gcy('organization-description-field').within(() =>
      cy.get('input').type('Very nice organization! Which is nice to create!')
    );
    clickGlobalSave();
    gcy('global-paginated-list').within(() =>
      cy.contains('What a nice organization')
    );
    cy.contains('Organization created').should('be.visible');
  });

  it('creates organization without description', () => {
    gcy('global-plus-button').click();
    gcy('organization-name-field').within(() =>
      cy.get('input').type('What a nice organization')
    );
    gcy('organization-address-part-field').within(() =>
      cy.get('input').should('have.value', 'what-a-nice-organization')
    );
    clickGlobalSave();
    cy.contains('Organization created').should('be.visible');
  });

  it('validates creation fields', () => {
    gcy('global-plus-button').click();
    gcy('organization-name-field').within(() => {
      cy.get('input').type('aaa').clear();
    });

    gcy('organization-address-part-field').click();

    gcy('organization-name-field').within(() => {
      cy.contains('This field is required');
    });

    gcy('organization-name-field').within(() => {
      cy.get('input').type(
        'This is too too too too too too too too too too too too too too too too too too long'
      );
      cy.contains('This field can contain at maximum 50 characters');
    });

    clickGlobalSave();
    cy.contains('Organization created').should('not.exist');

    gcy('organization-description-field').click();

    gcy('organization-address-part-field').contains('This field is required');
  });

  describe('list', () => {
    it('contains created data', () => {
      gcy('global-paginated-list').within(() => {
        cy.contains('Facebook').should('be.visible');
        cy.contains('ZZZ Cool company 10')
          .scrollIntoView()
          .should('be.visible');
        goToPage(2);
        cy.contains('ZZZ Cool company 14');
      });
    });

    it('admin cannot see microsoft settings button', () => {
      gcy('global-paginated-list').within(() => {
        cy.contains('Microsoft')
          .closest('li')
          .within(() => {
            gcy('organization-settings-button').should('not.exist');
          });
      });
    });

    it('admin leaves Microsoft', { scrollBehavior: 'center' }, () => {
      gcy('global-paginated-list').within(() => {
        cy.contains('Microsoft')
          .closest('li')
          .within(($li) => {
            gcy('leave-organization-button').click();
          });
      });
      gcy('global-confirmation-confirm').click();
      cy.contains('Organization left').should('be.visible');
    });

    it('admin cannot leave Techfides', { scrollBehavior: 'center' }, () => {
      gcy('global-paginated-list').within(() => {
        cy.contains('Techfides')
          .closest('li')
          .within(($li) => {
            gcy('leave-organization-button').click();
          });
      });
      gcy('global-confirmation-confirm').click();
      cy.contains('Organization has no other owner.').should('be.visible');
    });

    it('admin can access Tolgee settings', { scrollBehavior: 'center' }, () => {
      gcy('global-paginated-list').within(() => {
        cy.contains('Tolgee')
          .closest('li')
          .within(($li) => {
            gcy('organization-settings-button').click();
          });
      });
      cy.gcy('global-base-view-title').contains('Tolgee').should('be.visible');
    });
  });

  after(() => {
    organizationTestData.clean();
  });

  const visit = () => {
    cy.visit(`${HOST}/organizations`);
  };
});
