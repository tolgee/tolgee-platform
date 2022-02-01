declare namespace Cypress {
  import Value = DataCy.Value;

  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  interface Chainable<Subject = any> {
    closestDcy(dataCy: Value): Chainable;

    gcy(dataCy: Value): Chainable;

    findDcy(dataCy: Value): Chainable;

    nextUntilDcy(dataCy: Value): Chainable;

    findInputByName(name: string): Chainable;
  }
}
