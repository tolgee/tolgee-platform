import { pageAcessibleWithoutErrors, ProjectInfo } from './shared';

function tryClickable(item: DataCy.Value, clickable: boolean) {
  cy.gcy(item).then(($el) => {
    cy.gcy(item).click();
    pageAcessibleWithoutErrors();
    if (clickable) {
      cy.go('back');
      pageAcessibleWithoutErrors();
    }
  });
}

export function testDashboard(projectInfo: ProjectInfo) {
  const scopes = projectInfo.project.computedPermission.scopes;

  tryClickable(
    'project-dashboard-language-count',
    scopes.includes('languages.edit')
  );
  tryClickable('project-dashboard-text', scopes.includes('keys.view'));
  tryClickable('project-dashboard-progress', scopes.includes('keys.view'));
  tryClickable('project-dashboard-members', scopes.includes('members.view'));

  if (scopes.includes('activity.view')) {
    cy.gcy('project-dashboard-activity-list').should('be.visible');
    cy.gcy('project-dashboard-activity-chart').should('be.visible');
  }
}
