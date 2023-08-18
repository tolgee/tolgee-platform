import { HOST } from '../../common/constants';
import 'cypress-file-upload';
import { projectTestData } from '../../common/apiCalls/testData/testData';
import { login } from '../../common/apiCalls/common';
import { assertMessage } from '../../common/shared';
import { simulateError } from '../../common/errorHandling';

describe('Error handling', () => {
  beforeEach(() => {
    projectTestData.clean();
    projectTestData.generate();
    login('cukrberg@facebook.com', 'admin');
  });

  it('Handles not found error', () => {
    simulateError({
      method: 'get',
      endpoint: 'projects-with-stats',
      statusCode: 404,
    });
    cy.visit(`${HOST}`);
    assertMessage('Not found');
  });

  it('Handles permissions error', () => {
    simulateError({
      method: 'get',
      endpoint: 'projects-with-stats',
      statusCode: 403,
    });
    cy.visit(`${HOST}`);
    assertMessage('Your permissions are not sufficient for this operation.');
  });

  it('Handles 404 by redirect', () => {
    simulateError({
      method: 'get',
      endpoint: 'facebook',
      statusCode: 404,
    });
    cy.visit(`${HOST}/organizations/facebook/profile`);
    assertMessage('Not found');
    cy.url().should('include', '/projects');
  });

  it('Handles 401 by logout', () => {
    simulateError({
      method: 'get',
      endpoint: 'facebook',
      statusCode: 401,
    });
    cy.visit(`${HOST}/organizations/facebook/profile`);
    cy.url().should('include', '/login');
  });

  after(() => {
    projectTestData.clean();
  });
});
