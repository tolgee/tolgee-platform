import { login, setBypassSeatCountCheck } from '../../common/apiCalls/common';
import { projectSwitchingTestData } from '../../common/apiCalls/testData/testData';
import { HOST } from '../../common/constants';
import { waitForGlobalLoading } from '../../common/loading';
import { gcy } from '../../common/shared';

describe('Project switching', () => {
  beforeEach(() => {
    setBypassSeatCountCheck(true);
    projectSwitchingTestData.clean();
    projectSwitchingTestData.generateStandard().then((res) => {
      const project = res.body.projects.find(
        (item) => item.name === 'Current project'
      );
      login('projectSwitchingUser', 'admin');
      cy.visit(`${HOST}/projects/${project.id}/translations`);
      waitForGlobalLoading();
    });
  });

  afterEach(() => {
    projectSwitchingTestData.clean();
    setBypassSeatCountCheck(false);
  });

  it('switches to another project of the organization', () => {
    gcy('project-switch').click();
    gcy('switch-popover-item').contains('Other project 01').click();
    waitForGlobalLoading();

    gcy('navigation-item').contains('Other project 01').should('be.visible');
    cy.url().should('not.contain', '/translations');
  });

  it('keeps the project name linking to the dashboard', () => {
    gcy('navigation-item').contains('Current project').click();
    waitForGlobalLoading();

    gcy('project-dashboard-language-count').should('be.visible');
  });

  it('filters projects by search', () => {
    gcy('project-switch').click();
    gcy('switch-popover-search').type('Other project 07');
    waitForGlobalLoading();

    gcy('switch-popover-item')
      .should('have.length', 1)
      .contains('Other project 07');
  });

  it('loads the next page of projects', () => {
    gcy('project-switch').click();
    gcy('switch-popover-item').should('have.length', 20);

    gcy('switch-popover-load-more').click();
    waitForGlobalLoading();

    gcy('switch-popover-item').should('have.length', 22);
  });
});
