import {
  create4Translations,
  translationsBeforeEach,
} from '../../common/translations';
import { ProjectDTO } from '../../../../webapp/src/service/response.types';
import { waitForGlobalLoading } from '../../common/loading';

describe('Key description', () => {
  let project: ProjectDTO = null;

  beforeEach(() => {
    translationsBeforeEach()
      .then((p) => (project = p))
      .then(() => create4Translations(project.id));
  });

  it('disables english', () => {
    const description = 'This cool key is just for e2e tests';
    cy.gcy('translations-table-cell').contains('Cool key 01').click();
    cy.gcy('translations-key-edit-description-field').type(description);
    cy.gcy('translations-cell-main-action-button').click();
    waitForGlobalLoading();

    cy.gcy('translations-key-cell-description')
      .contains(description)
      .should('be.visible');
  });

  it('renders markdown in key description correctly', () => {
    const markdownDescription =
      '**Bold text** and [link](https://tolgee.io) and *italic text*';
    cy.gcy('translations-table-cell').contains('Cool key 01').click();
    cy.gcy('translations-key-edit-description-field')
      .clear()
      .type(markdownDescription);
    cy.gcy('translations-cell-main-action-button').click();
    waitForGlobalLoading();

    cy.gcy('translations-key-cell-description').within(() => {
      cy.get('strong').contains('Bold text').should('be.visible');
      cy.get('a')
        .should('have.attr', 'href', 'https://tolgee.io')
        .contains('link');
      cy.get('em').contains('italic text').should('be.visible');
    });
  });
});
