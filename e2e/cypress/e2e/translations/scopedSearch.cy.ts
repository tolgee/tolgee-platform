import {
  create4Translations,
  translationsBeforeEach,
} from '../../common/translations';
import { ProjectDTO } from '../../../../webapp/src/service/response.types';
import { waitForGlobalLoading } from '../../common/loading';

const searchInput = () => cy.gcy('global-search-field').find('.cm-content');

describe('Scoped search', () => {
  let project: ProjectDTO = null;

  beforeEach(() => {
    translationsBeforeEach()
      .then((p) => (project = p))
      .then(() => create4Translations(project.id));
  });

  it('searches only in key descriptions', () => {
    cy.gcy('translations-table-cell').contains('Cool key 01').click();
    cy.gcy('translations-key-edit-description-field').type(
      'wombat terminology'
    );
    cy.gcy('translations-cell-main-action-button').click();
    waitForGlobalLoading();

    searchInput().type('description:wombat');
    cy.contains('Cool key 01').should('be.visible');
    cy.contains('Cool key 02').should('not.exist');

    // "Cool" matches other keys by name, but not their descriptions
    searchInput().clear().type('description:Cool');
    cy.contains('Cool key 01').should('not.exist');
  });

  it('ignores a qualifier while its value is still empty', () => {
    searchInput().type('description:');
    waitForGlobalLoading();
    cy.contains('Cool key 01').should('be.visible');
    cy.contains('Cool key 04').should('be.visible');
  });

  it('searches keys by wildcard patterns', () => {
    searchInput().type('key:*01');
    cy.contains('Cool key 01').should('be.visible');
    cy.contains('Cool key 02').should('not.exist');

    searchInput().clear().type('key:Cool*');
    cy.contains('Cool key 01').should('be.visible');
    cy.contains('Cool key 04').should('be.visible');
  });

  it('searches in a specific language', () => {
    searchInput().type('cs:"Studený přeložený text 1"');
    cy.contains('Cool key 01').should('be.visible');
    cy.contains('Cool key 02').should('not.exist');

    searchInput().clear().type('en:"Studený přeložený text"');
    cy.contains('Cool key 01').should('not.exist');
  });

  it('keeps plain search behavior', () => {
    searchInput().type('Cool key 04');
    cy.contains('Cool key 01').should('not.exist');
    cy.contains('Cool key 04').should('be.visible');
  });

  it('shows syntax help popover with docs link', () => {
    cy.gcy('translations-search-help-button').click();
    cy.gcy('translations-search-help-popover')
      .should('be.visible')
      .and('contain', 'description:cart');
    cy.gcy('translations-search-help-docs-link')
      .should('have.attr', 'href')
      .and('include', 'docs.tolgee.io');
  });

  it('suggests qualifiers after two letters and completes on click', () => {
    searchInput().type('d');
    cy.get('.cm-tooltip-autocomplete').should('not.exist');

    searchInput().type('e');
    cy.get('.cm-tooltip-autocomplete')
      .should('be.visible')
      .contains('description:')
      .click();
    searchInput().should('have.text', 'description:');
  });

  it('completes qualifier with keyboard', () => {
    searchInput().type('key');
    cy.get('.cm-tooltip-autocomplete').should('be.visible');
    searchInput().type('{enter}');
    searchInput().should('have.text', 'key:');
    searchInput().type('*01');
    cy.contains('Cool key 01').should('be.visible');
    cy.contains('Cool key 02').should('not.exist');
  });
});
