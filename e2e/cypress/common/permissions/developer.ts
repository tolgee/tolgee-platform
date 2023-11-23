import { ProjectInfo } from './shared';

export function testDeveloper(projectInfo: ProjectInfo) {
  const scopes = projectInfo.project.computedPermission.scopes;
  if (scopes.includes('content-delivery.publish')) {
    cy.gcy('content-delivery-subtitle').should('be.visible');
    if (scopes.includes('content-delivery.manage')) {
      cy.gcy('content-delivery-add-button').should('not.be.disabled');
      cy.gcy('developer-menu-storage').click();
      cy.gcy('storage-add-item-button').should('not.be.disabled');
    }
  } else {
    cy.gcy('webhooks-subtitle').should('be.visible');
  }

  if (scopes.includes('webhooks.manage')) {
    cy.gcy('developer-menu-webhooks').click();
    cy.gcy('webhooks-add-item-button').should('not.be.disabled');
  }
}
