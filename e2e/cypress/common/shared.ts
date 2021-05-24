/// <reference types="cypress" />
import {getAnyContainingAriaLabelAttribute} from "./xPath";
import {Scope} from "./types";
import Value = DataCy.Value;
import Chainable = Cypress.Chainable;

export const allScopes: Scope[] = ["keys.edit", "translations.edit", "translations.view"];

export const clickAdd = () => {
    cy.wait(100);
    cy.xpath(getAnyContainingAriaLabelAttribute("add")).click();
};

export const getPopover = () => {
    return cy.xpath("//*[contains(@class, 'MuiPopover-root') and not(contains(@style, 'visibility'))]")
}

export const getDialog = () => {
    return cy.xpath("//*[contains(@class, 'MuiDialog-root')]")
}

export const gcy = (dataCy: Value) => cy.get('[data-cy="' + dataCy + '"]')
export const goToPage = (page: number) => gcy("global-list-pagination").within(() => cy.xpath(".//button[text() = '" + page + "']").click())


export const clickGlobalSave = () => {
    gcy("global-form-save-button").click()
}

export const confirmHardMode = () => {
    gcy("global-confirmation-hard-mode-text-field").within(() => {
        cy.get("label").then(($label) => {
            cy.get("input").type($label.text().replace("Rewrite text: ", ""))
        }).its("text")
    })
    gcy("global-confirmation-confirm").click()
}

export const confirmStandard = () => {
    gcy("global-confirmation-confirm").click()
}

export const assertMessage = (message: string) => {
    gcy("global-snackbars").should("contain", message)
}

export const selectInRepositoryMenu = (itemName: string) => {
    gcy("repository-menu-items").contains(itemName).click()
}

export const selectInSelect = (chainable: Chainable, renderedValue: string) => {
    chainable.find("div").first().click()
    getPopover().contains(renderedValue).click()
}
