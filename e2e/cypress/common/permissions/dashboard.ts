import { pageIsPermitted, ProjectInfo } from './shared';

function tryClickIfClickable(item: DataCy.Value) {
  cy.gcy(item).then(($el) => {
    if ($el.hasClass('clickable')) {
      cy.gcy(item).click();
      pageIsPermitted();
      cy.go('back');
      pageIsPermitted();
    }
  });
}

export function testDashboard(projectInfo: ProjectInfo) {
  const scopes = projectInfo.project.computedPermission.scopes;
  tryClickIfClickable('project-dashboard-language-count');
  tryClickIfClickable('project-dashboard-text');
  tryClickIfClickable('project-dashboard-progress');
  tryClickIfClickable('project-dashboard-members');
  if (scopes.includes('activity.view')) {
    cy.gcy('project-dashboard-activity-list').should('be.visible');
    cy.gcy('project-dashboard-activity-chart').should('be.visible');
  }
}
