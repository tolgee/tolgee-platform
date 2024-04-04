import { waitForGlobalLoading } from './loading';
import { confirmStandard } from './shared';

export function selectAll() {
  cy.gcy('translations-row-checkbox').first().click();
  cy.gcy('translations-select-all-button').first().click({ timeout: 20_000 });
  waitForGlobalLoading(500);
}

export function openBatchOperationMenu() {
  cy.gcy('batch-operations-select').click({ timeout: 20_000 });
}

export function findBatchOperation(operation: string) {
  return cy.gcy('batch-select-item').contains(operation);
}

export function selectOperation(operation: string) {
  openBatchOperationMenu();
  findBatchOperation(operation).click();
}

export function executeBatchOperationWithConfirmation() {
  cy.gcy('batch-operations-submit-button').click();
  confirmStandard();
  cy.waitForDom();
  cy.gcy('batch-operation-dialog-ok', { timeout: 100_000 }).click();
}

export function executeBatchOperation() {
  cy.gcy('batch-operations-submit-button').click();
  cy.waitForDom();
  cy.gcy('batch-operation-dialog-ok', { timeout: 100_000 }).click();
}

export function deleteSelected() {
  selectOperation('Delete');
  executeBatchOperationWithConfirmation();
}
