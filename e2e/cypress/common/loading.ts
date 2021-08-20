export const expectGlobalLoading = () => {
  cy.gcy('global-loading').should('exist');
  return cy.gcy('global-loading').should('not.exist');
};

export const waitForGlobalLoading = () => {
  cy.wait(100);
  return cy.gcy('global-loading').should('not.exist');
};
