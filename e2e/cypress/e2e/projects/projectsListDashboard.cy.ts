import { HOST } from '../../common/constants';
import 'cypress-file-upload';
import { assertTooltip, gcy } from '../../common/shared';
import { projectListData } from '../../common/apiCalls/testData/testData';
import { login, setBypassSeatCountCheck } from '../../common/apiCalls/common';

describe('Projects Dashboard', () => {
  beforeEach(() => {
    setBypassSeatCountCheck(true);
    projectListData.clean();
    projectListData.generate();
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
      .should('have.length', 2)
      //wait for animation
      .wait(500)
      .should('be.visible');
    getNthStateInProject(project2, 0).trigger('mouseover');
    assertTooltip('Translated');
    getNthStateInProject(project2, 0).trigger('mouseout');
    getNthStateInProject(project2, 1).trigger('mouseover');
    assertTooltip('Untranslated');
  });

  it('shows languages', () => {
    const project2 = 'Project 2';
    getNthLanguageInProject(project2, 0).trigger('mouseover');
    assertTooltip('English');
    getNthLanguageInProject(project2, 0).trigger('mouseout');
    getNthLanguageInProject(project2, 1).trigger('mouseover');
    assertTooltip('Deutsch');
  });

  afterEach(() => {
    projectListData.clean();
    setBypassSeatCountCheck(false);
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
    .findDcy('language-icon-list-item')
    .should('be.visible')
    .eq(nth);
};
