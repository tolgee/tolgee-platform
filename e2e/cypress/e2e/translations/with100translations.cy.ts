import { ProjectDTO } from '../../../../webapp/src/service/response.types';
import {
  translationsBeforeEach,
  visitTranslations,
} from '../../common/translations';
import { waitForGlobalLoading } from '../../common/loading';
import { generateExampleKeys } from '../../common/apiCalls/testData/testData';
import { deleteProject } from '../../common/apiCalls/common';

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
    waitForGlobalLoading();
  });

  afterEach(() => {
    deleteProject(project.id);
  });

  it('will scroll properly in list view', () => {
    testScroll();
  });

  it('will scroll properly in table view', () => {
    cy.gcy('translations-view-table-button').click();
    testScroll();
  });

  const testScroll = () => {
    cy.gcy('translations-toolbar-to-top').should('not.be.visible');
    cy.gcy('translations-key-count').contains('100').should('be.visible');
    cy.scrollTo('bottom');
    // it should load key 60 on bottom of the page if, heights of cells are estimated correctly
    cy.contains('Cool key 60').should('exist');
    //wait for toolbar to show after transition
    cy.wait(500);
    cy.gcy('translations-toolbar-to-top').should('be.visible');
    // now we scroll to the end of newly loaded data
    cy.scrollTo('bottom');
    cy.contains('Cool translated text 99').should('be.visible');
    cy.gcy('translations-toolbar-counter')
      .contains('100 / 100')
      .should('be.visible');
    cy.gcy('translations-toolbar-to-top').click();
    cy.contains('Cool key 00').should('be.visible');
  };

  const visit = () => {
    visitTranslations(project.id);
  };
});
