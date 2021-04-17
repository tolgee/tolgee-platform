declare namespace Cypress {
    import Value = DataCy.Value;

    interface Chainable<Subject = any> {
        closestDcy(dataCy: Value): Chainable
        gcy(dataCy: Value): Chainable
        findDcy(dataCy: Value): Chainable
        findInputByName(name: String): Chainable
    }
}

