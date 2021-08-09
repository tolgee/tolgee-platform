import { ProjectDTO } from '../../../../../webapp/src/service/response.types';
import {
  create4Translations,
  translationsBeforeEach,
} from '../../../common/translations';

describe('Tags with 5 translations', () => {
  let project: ProjectDTO = null;

  beforeEach(() => {
    translationsBeforeEach()
      .then((p) => (project = p))
      .then(() => create4Translations(project.id));
  });

  it('will create new tag', () => {
    createTag('testTag');
    cy.contains('testTag').should('be.visible');
  });

  it('will remove tag', () => {
    createTag('testTag');
    cy.contains('testTag').should('be.visible');
    cy.gcy('translations-tag-close').click();
    cy.contains('testTag').should('not.exist');
  });

  it('will reuse existing tag', () => {
    createTag('testTag');
    cy.gcy('translations-tags-add').eq(1).click();
    cy.focused().type('test');
    cy.gcy('tag-autocomplete-option').contains('testTag').should('be.visible');
    cy.contains('Add "test"').should('be.visible');
    cy.gcy('tag-autocomplete-option').contains('testTag').click();
    // wait for loading to disappear
    cy.gcy('global-base-view-loading').should('not.exist');
    cy.gcy('translations-tag')
      .should('have.length', 2)
      .each((el) => cy.wrap(el).contains('testTag').should('be.visible'));
  });
});

function createTag(name: string) {
  cy.gcy('translations-tags-add').first().click();
  cy.focused().type(name);
  cy.gcy('tag-autocomplete-option').contains('Add').click();
  // wait for loading to disappear
  cy.gcy('global-base-view-loading').should('not.exist');
}
