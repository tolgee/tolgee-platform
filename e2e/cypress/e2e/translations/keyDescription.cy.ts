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
    cy.gcy('translations-cell-save-button').click();
    waitForGlobalLoading();

    cy.gcy('translations-key-cell-description')
      .contains(description)
      .should('be.visible');
  });
});
