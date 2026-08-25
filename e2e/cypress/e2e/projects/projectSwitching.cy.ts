import { login, setBypassSeatCountCheck } from '../../common/apiCalls/common';
import { projectListData } from '../../common/apiCalls/testData/testData';
import { waitForGlobalLoading } from '../../common/loading';
import { enterProject } from '../../common/projects';
import { gcy } from '../../common/shared';

describe('Project switching', () => {
  beforeEach(() => {
    setBypassSeatCountCheck(true);
    projectListData.clean();
    projectListData.generate();
    login('projectListDashboardUser', 'admin');
    enterProject('test_project');
  });

  afterEach(() => {
    projectListData.clean();
    setBypassSeatCountCheck(false);
  });

  it('switches to another project of the organization', () => {
    gcy('project-switch').click();
    gcy('switch-popover-item').contains('Project 2').click();
    waitForGlobalLoading();

    gcy('navigation-item').contains('Project 2').should('be.visible');
    cy.url().should('not.contain', '/translations');
  });

  it('keeps the project name linking to the dashboard', () => {
    gcy('navigation-item').contains('test_project').click();
    waitForGlobalLoading();

    gcy('project-dashboard-language-count').should('be.visible');
  });
});
