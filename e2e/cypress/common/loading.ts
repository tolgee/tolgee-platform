export const expectGlobalLoading = () => {
  cy.gcy('global-loading').should('exist');
  return cy.gcy('global-loading').should('not.exist');
};

export const waitForGlobalLoading = (waitTime = 100, timeout = 4000) => {
  cy.wait(waitTime);
  return cy.gcy('global-loading').should('not.exist', { timeout });
};
