import { ProjectDTO } from '../../../../webapp/src/service/response.types';
import {
  translationsBeforeEach,
  visitTranslations,
} from '../../common/translations';
import { waitForGlobalLoading } from '../../common/loading';
import { generateExampleKeys } from '../../common/apiCalls/testData/testData';
import { deleteProject } from '../../common/apiCalls/common';
import { gcy } from '../../common/shared';
import {
  selectOperation,
  executeBatchOperation,
  executeBatchOperationWithConfirmation,
} from '../../common/batchJobs';

describe('Group actions', () => {
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

  it('will delete all properly', () => {
    gcy('translations-row-checkbox').first().click();
    gcy('translations-select-all-button').click();
    waitForGlobalLoading(500);
    selectOperation('Delete');
    executeBatchOperationWithConfirmation();
    waitForGlobalLoading(500);
    gcy('global-empty-list').should('be.visible');
  });

  it('will delete all except first one', () => {
    gcy('translations-row-checkbox').first().click();
    gcy('translations-select-all-button').click();
    waitForGlobalLoading();
    gcy('translations-row-checkbox').first().click();
    selectOperation('Delete');
    executeBatchOperationWithConfirmation();
    waitForGlobalLoading(500);
    gcy('translations-key-count').contains('1').should('be.visible');
  });

  const visit = () => {
    visitTranslations(project.id);
  };
});
