declare namespace Cypress {
  import Value = DataCy.Value;

  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  interface Chainable<Subject = any> {
    closestDcy(dataCy: Value): Chainable;

    siblingDcy(dataCy: Value): Chainable;

    gcy(dataCy: Value, options?: Parameters<typeof cy['get']>[1]): Chainable;

    findDcy(
      dataCy: Value,
      options?: Parameters<typeof cy['find']>[1]
    ): Chainable;

    nextUntilDcy(dataCy: Value): Chainable;

    findInputByName(name: string): Chainable;

    waitForDom(): Chainable;

    chooseDatePicker(selector: string, value: string): void;
  }
}
