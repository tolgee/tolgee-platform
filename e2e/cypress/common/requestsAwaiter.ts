let pendingRequests = 0;

/**
 * Counts pending requests
 * causes issues when there is another intercept used in the test so use carefully
 */
export function setupRequestAwaiter(filter = '**') {
  pendingRequests = 0;
  cy.intercept(filter, { middleware: true }, (req) => {
    pendingRequests += 1;
    req.continue(() => {
      pendingRequests -= 1;
    });
  }).as('request');
}

/**
 * Waits for pending requests to finish if there are some
 */
export function awaitPendingRequests() {
  if (pendingRequests > 0) {
    cy.wait('@request');
  }
}
