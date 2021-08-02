import { deleteProject, generateExampleKeys } from '../../common/apiCalls';
import { ProjectDTO } from '../../../../webapp/src/service/response.types';
import {
  translationsBeforeEach,
  visitTranslations,
} from '../../common/translations';

describe('With 100 translations', () => {
  let project: ProjectDTO = null;

  beforeEach(() => {
    translationsBeforeEach().then((p) => (project = p));
    cy.wrap(null).then(() =>
      Cypress.Promise.all([generateExampleKeys(project.id, 100)]).then(() => {
        visit();
      })
    );

    // wait for loading to appear and disappear again
    cy.gcy('global-base-view-content').should('be.visible');
    cy.gcy('global-base-view-loading').should('not.exist');
  });

  afterEach(() => {
    deleteProject(project.id);
  });

  it('will scroll properly in list view', () => {
    cy.gcy('global-project-scrollable-content').scrollTo('bottom');
    // it should load key 60 on bottom of the page if, heights of cells are estimated correctly
    cy.contains('Cool key 60').should('exist');
    // now we scroll to the end of newly loaded data
    cy.gcy('global-project-scrollable-content').scrollTo('bottom');
    cy.contains('Cool translated text 99').should('be.visible');
  });

  it('will scroll properly in table view', () => {
    cy.gcy('translations-view-table-button').click();
    cy.gcy('global-project-scrollable-content').scrollTo('bottom');
    // it should load key 60 on bottom of the page if, heights of cells are estimated correctly
    cy.contains('Cool key 60').should('exist');
    // now we scroll to the end of newly loaded data
    cy.gcy('global-project-scrollable-content').scrollTo('bottom');
    cy.contains('Cool translated text 99').should('be.visible');
  });

  const visit = () => {
    visitTranslations(project.id);
  };
});
