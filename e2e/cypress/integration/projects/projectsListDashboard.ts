import { HOST } from '../../common/constants';
import 'cypress-file-upload';
import { assertTooltip, gcy } from '../../common/shared';
import { projectsDashboardData } from '../../common/apiCalls/testData/testData';
import { login } from '../../common/apiCalls/common';

describe('Projects Dashboard', () => {
  before(() => {
    projectsDashboardData.clean();
    projectsDashboardData.generate();
    login('projectListDashboardUser', 'admin');
    cy.visit(`${HOST}`);
  });

  it('contains key count', () => {
    gcy('dashboard-projects-list-item').contains('1 key');
    gcy('dashboard-projects-list-item').contains('5 keys');
  });

  it('contains progress bar', () => {
    const project2 = 'Project 2';
    cy.contains(project2)
      .closestDcy('dashboard-projects-list-item')
      .should('be.visible')
      .findDcy('project-states-bar-bar')
      .should('be.visible')
      .findDcy('project-states-bar-state-progress')
      .should('have.length', 3)
      //wait for animation
      .wait(500)
      .should('be.visible');
    getNthStateInProject(project2, 0).trigger('mouseover');
    assertTooltip('Reviewed');
    getNthStateInProject(project2, 0).trigger('mouseout');
    getNthStateInProject(project2, 1).trigger('mouseover');
    assertTooltip('Translated');
  });

  it('shows languages', () => {
    const project2 = 'Project 2';
    getNthLanguageInProject(project2, 0).trigger('mouseover');
    assertTooltip('English');
    getNthLanguageInProject(project2, 0).trigger('mouseout');
    getNthLanguageInProject(project2, 1).trigger('mouseover');
    assertTooltip('Deutsch');
  });

  after(() => {
    projectsDashboardData.clean();
  });
});

const getNthStateInProject = (projectName: string, nth: number) => {
  return cy
    .contains(projectName)
    .closestDcy('dashboard-projects-list-item')
    .findDcy('project-states-bar-state-progress')
    .should('be.visible')
    .eq(nth);
};

const getNthLanguageInProject = (projectName: string, nth: number) => {
  return cy
    .contains(projectName)
    .closestDcy('dashboard-projects-list-item')
    .should('be.visible')
    .findDcy('project-list-languages-item')
    .should('be.visible')
    .eq(nth);
};
