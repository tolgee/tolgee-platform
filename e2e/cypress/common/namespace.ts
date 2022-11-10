export function selectNamespace(namespace: string) {
  cy.gcy('namespaces-select').click();
  cy.gcy('namespaces-select-option-new').click();
  cy.gcy('namespaces-select-text-field').type(namespace);
  return cy.gcy('global-confirmation-confirm').click();
}
