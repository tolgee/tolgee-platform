export function selectNamespace(namespace: string) {
  cy.gcy('namespaces-selector').click();
  cy.gcy('search-select-new').click();
  cy.gcy('namespaces-select-text-field').type(namespace);
  return cy.gcy('namespaces-select-confirm').click();
}
