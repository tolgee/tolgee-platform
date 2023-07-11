import { confirmStandard } from './shared';

export function deleteSelected() {
  cy.gcy('batch-operations-select').click();
  cy.gcy('batch-select-item').contains('Delete').click();
  cy.gcy('batch-operations-submit-button').click();
  confirmStandard();
  cy.wait(2000);
  cy.gcy('batch-operation-dialog-ok', { timeout: 10_000 }).click();
}
