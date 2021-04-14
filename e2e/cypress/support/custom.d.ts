declare namespace Cypress {
    import Value = DataCy.Value;

    interface Chainable<Subject = any> {
        closestDataCy(dataCy: Value): Chainable
        gcy(dataCy: Value): Chainable
    }
}

