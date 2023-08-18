import { HOST } from '../../common/constants';
import 'cypress-file-upload';
import { projectTestData } from '../../common/apiCalls/testData/testData';
import { login } from '../../common/apiCalls/common';
import { assertMessage } from '../../common/shared';
import { simulateError, tryCreateProject } from '../../common/errorHandling';

describe('Error handling', () => {
  beforeEach(() => {
    projectTestData.clean();
    projectTestData.generate();
    login('cukrberg@facebook.com', 'admin');
  });

  it('Handles project creation general error', () => {
    simulateError({
      method: 'post',
      endpoint: 'projects',
      statusCode: 400,
      body: {
        code: 'user_has_no_project_access',
      },
    });
    cy.visit(`${HOST}/projects/add`);
    tryCreateProject('Test');
    cy.contains('User has no access to the project').should('be.visible');
  });

  it('Handles project creation 403 error', () => {
    simulateError({
      method: 'post',
      endpoint: 'projects',
      statusCode: 403,
    });
    cy.visit(`${HOST}/projects/add`);
    tryCreateProject('Test');
    assertMessage('Your permissions are not sufficient for this operation');
  });

  it('Handles project creation 404 error without redirect', () => {
    simulateError({
      method: 'post',
      endpoint: 'projects',
      statusCode: 404,
    });
    cy.visit(`${HOST}/projects/add`);
    tryCreateProject('Test');
    assertMessage('Not found');
    cy.url().should('contain', '/projects/add');
  });

  after(() => {
    projectTestData.clean();
  });
});
