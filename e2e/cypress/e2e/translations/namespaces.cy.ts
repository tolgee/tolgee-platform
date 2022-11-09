import { waitForGlobalLoading } from '../../common/loading';
import { namespaces } from '../../common/apiCalls/testData/testData';
import { login } from '../../common/apiCalls/common';
import {
  createTranslation,
  visitTranslations,
} from '../../common/translations';
import { gcy } from '../../common/shared';

describe('namespaces in translations', () => {
  beforeEach(() => {
    namespaces.clean();
    namespaces
      .generateStandard()
      .then((r) => r.body)
      .then(({ users, projects }) => {
        login(users[0].username);
        const testProject = projects.find(
          ({ name }) => name === 'test_project'
        );
        visitTranslations(testProject.id);
      });
    waitForGlobalLoading();
  });

  afterEach(() => {
    namespaces.clean();
  });

  it('displays keys with namespaces correctly', () => {
    gcy('translations-namespace-banner').contains('ns-1').should('be.visible');
    gcy('translations-namespace-banner').contains('ns-2').should('be.visible');
  });

  it('displays <default>', () => {
    createTranslation('new-key', undefined, undefined, 'new-ns');
    gcy('translations-namespace-banner')
      .contains('new-ns')
      .should('be.visible');
    gcy('translations-namespace-banner')
      .contains('<default>')
      .should('be.visible');
  });

  it('edits namespaced correctly', () => {
    gcy('translations-namespace-banner')
      .contains('ns-1')
      .nextUntilDcy('translations-namespace-banner')
      .findDcy('translations-table-cell')
      .contains('hello')
      .click();
    cy.gcy('global-editor').type(' edited translation').type('{enter}');
    waitForGlobalLoading();
    cy.gcy('translations-table-cell')
      .contains('edited translation')
      .should('be.visible');
  });

  it('updates namespace correctly', () => {
    gcy('translations-namespace-banner')
      .contains('ns-1')
      .nextUntilDcy('translations-namespace-banner')
      .findDcy('translations-table-cell')
      .first()
      .click();

    cy.gcy('namespaces-select-text-field').find('input').type('new-ns');
    cy.gcy('namespaces-select-option').contains('new-ns').click();

    cy.gcy('translations-cell-save-button').click();

    cy.gcy('translations-namespace-banner')
      .contains('new-ns')
      .should('be.visible');
  });
});
