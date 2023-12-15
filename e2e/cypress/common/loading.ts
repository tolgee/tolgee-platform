export const expectGlobalLoading = () => {
  cy.gcy('global-loading').should('exist');
  return cy.gcy('global-loading').should('not.exist');
};

export const waitForGlobalLoading = (
  waitTime = 100,
  timeout = Cypress.config('defaultCommandTimeout')
) => {
  cy.wait(waitTime);
  return cy.gcy('global-loading', { timeout }).should('not.exist');
};
