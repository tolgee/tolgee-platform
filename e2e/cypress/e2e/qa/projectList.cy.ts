import { qaTestData } from '../../common/apiCalls/testData/testData';
import { login } from '../../common/apiCalls/common';
import { waitForGlobalLoading } from '../../common/loading';
import { HOST } from '../../common/constants';

describe('QA on project list', () => {
  beforeEach(() => {
    qaTestData.clean();
    qaTestData.generateStandard();
    login('test_username');
  });

  afterEach(() => {
    qaTestData.clean();
  });

  it('shows QA badge on project with issues', () => {
    cy.visit(`${HOST}/projects`);
    waitForGlobalLoading();

    cy.contains('test_project')
      .closestDcy('dashboard-projects-list-item')
      .findDcy('project-list-qa-badge-button')
      .should('exist');
  });

  it('does not show QA badge on project with QA disabled', () => {
    cy.visit(`${HOST}/projects`);
    waitForGlobalLoading();

    cy.contains('Disabled QA Project')
      .closestDcy('dashboard-projects-list-item')
      .findDcy('project-list-qa-badge-button')
      .should('not.exist');
  });
});
