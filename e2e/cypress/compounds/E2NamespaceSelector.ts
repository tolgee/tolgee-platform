export class E2NamespaceSelector {
  getNamespaceSelect() {
    return cy.gcy('search-select');
  }

  selectNamespace(namespace: string) {
    this.getNamespaceSelect().click();
    cy.gcy('search-select-new').click();
    cy.gcy('namespaces-select-text-field').type(namespace);
    return cy.gcy('namespaces-select-confirm').click();
  }
}

export const selectNamespace = (namespace: string) => {
  new E2NamespaceSelector().selectNamespace(namespace);
};
