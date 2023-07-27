import { waitForGlobalLoading } from './loading';
import { confirmStandard } from './shared';

export function selectAll() {
  cy.gcy('translations-row-checkbox').first().click();
  cy.gcy('translations-select-all-button').click();
  waitForGlobalLoading(500);
}

export function selectOperation(operation: string) {
  cy.gcy('batch-operations-select').click();
  cy.gcy('batch-select-item').contains(operation).click();
}

export function executeBatchOperationWithConfirmation() {
  cy.gcy('batch-operations-submit-button').click();
  confirmStandard();
  cy.waitForDom();
  cy.gcy('batch-operation-dialog-ok', { timeout: 10_000 }).click();
}

export function executeBatchOperation() {
  cy.gcy('batch-operations-submit-button').click();
  cy.waitForDom();
  cy.gcy('batch-operation-dialog-ok', { timeout: 10_000 }).click();
}

export function deleteSelected() {
  selectOperation('Delete');
  executeBatchOperationWithConfirmation();
}
