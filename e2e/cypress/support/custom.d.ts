declare namespace Cypress {
  import Value = DataCy.Value;

  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  interface Chainable<Subject = any> {
    closestDcy(dataCy: Value): Chainable;

    gcy(
      dataCy: Value,
      options?: Partial<Loggable & Timeoutable & Withinable & Shadow>
    ): Chainable;

    findDcy(dataCy: Value): Chainable;

    nextUntilDcy(dataCy: Value): Chainable;

    findInputByName(name: string): Chainable;

    waitForDom(): Chainable;

    chooseDatePicker(selector: string, value: string): void;
  }
}
