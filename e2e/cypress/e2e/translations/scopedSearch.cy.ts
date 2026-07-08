import {
  create4Translations,
  translationsBeforeEach,
} from '../../common/translations';
import { ProjectDTO } from '../../../../webapp/src/service/response.types';
import { waitForGlobalLoading } from '../../common/loading';

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

    cy.gcy('global-search-field').type('description:wombat');
    cy.contains('Cool key 01').should('be.visible');
    cy.contains('Cool key 02').should('not.exist');

    // "Cool" matches other keys by name, but not their descriptions
    cy.gcy('global-search-field')
      .find('input')
      .clear()
      .type('description:Cool');
    cy.contains('Cool key 01').should('not.exist');
  });

  it('searches keys by wildcard patterns', () => {
    cy.gcy('global-search-field').type('key:*01');
    cy.contains('Cool key 01').should('be.visible');
    cy.contains('Cool key 02').should('not.exist');

    cy.gcy('global-search-field').find('input').clear().type('key:Cool*');
    cy.contains('Cool key 01').should('be.visible');
    cy.contains('Cool key 04').should('be.visible');
  });

  it('searches in a specific language', () => {
    cy.gcy('global-search-field').type('cs:"Studený přeložený text 1"');
    cy.contains('Cool key 01').should('be.visible');
    cy.contains('Cool key 02').should('not.exist');

    cy.gcy('global-search-field')
      .find('input')
      .clear()
      .type('en:"Studený přeložený text"');
    cy.contains('Cool key 01').should('not.exist');
  });

  it('keeps plain search behavior', () => {
    cy.gcy('global-search-field').type('Cool key 04');
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

  it('suggests qualifiers while typing and completes on click', () => {
    cy.gcy('global-search-field').type('des');
    cy.gcy('translations-search-suggestion-item')
      .contains('description:')
      .click();
    cy.gcy('global-search-field')
      .find('input')
      .should('have.value', 'description:');
  });

  it('completes qualifier with keyboard', () => {
    cy.gcy('global-search-field').type('key{enter}');
    cy.gcy('global-search-field').find('input').should('have.value', 'key:');
    cy.focused().type('*01');
    cy.contains('Cool key 01').should('be.visible');
    cy.contains('Cool key 02').should('not.exist');
  });
});
