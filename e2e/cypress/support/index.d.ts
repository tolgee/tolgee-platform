declare namespace Cypress {
  interface Chainable<Subject = any> {
    promisify(): Promise<Subject>;
  }
}
