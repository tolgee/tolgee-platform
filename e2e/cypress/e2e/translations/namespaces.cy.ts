import { waitForGlobalLoading } from '../../common/loading';
import { namespaces } from '../../common/apiCalls/testData/testData';
import { login } from '../../common/apiCalls/common';
import {
  createTranslation,
  visitTranslations,
} from '../../common/translations';
import { gcy, getPopover, selectInSelect } from '../../common/shared';
import { selectNamespace } from '../../common/namespace';

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

    selectNamespace('new-ns');

    cy.gcy('translations-cell-save-button').click();

    cy.gcy('translations-namespace-banner')
      .contains('new-ns')
      .should('be.visible');
  });

  it('filters by clicking on banner', () => {
    gcy('translations-key-count').contains('5 Keys').should('be.visible');
    gcy('translations-namespace-banner').contains('ns-1').click();
    gcy('translations-key-count').contains('2 Keys').should('be.visible');
    gcy('translations-namespace-banner').contains('ns-1').click();
    gcy('translations-key-count').contains('5 Keys').should('be.visible');
  });

  it('filters by empty namespace', () => {
    gcy('translations-key-count').contains('5 Keys').should('be.visible');
    selectInSelect(gcy('translations-filter-select'), 'Namespaces');
    getPopover().contains('<default>').click();
    cy.focused().type('{Esc}');
    cy.focused().type('{Esc}');
    gcy('translations-key-count').contains('2 Keys').should('be.visible');
  });

  it('filters by multiple namespaces', () => {
    gcy('translations-key-count').contains('5 Keys').should('be.visible');
    selectInSelect(gcy('translations-filter-select'), 'Namespaces');
    getPopover().contains('ns-1').click();
    getPopover().contains('ns-2').click();
    cy.focused().type('{Esc}');
    cy.focused().type('{Esc}');
    gcy('translations-key-count').contains('3 Keys').should('be.visible');
  });
});
