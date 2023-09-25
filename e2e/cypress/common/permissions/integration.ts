import { dismissMenu } from '../shared';
import { ProjectInfo } from './shared';

export function testIntegration({ project }: ProjectInfo) {
  // check that api key dialog works and allows to select at least one scope
  cy.gcy('integrate-weapon-selector-button').contains('React').click();
  cy.gcy('integrate-api-key-selector-select').click();
  cy.gcy('integrate-api-key-selector-create-new-item').click();
  cy.gcy('permissions-advanced-checkbox').should('exist');
  dismissMenu();
}
