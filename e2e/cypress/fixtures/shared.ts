/// <reference types="cypress" />
import {getAnyContainingAriaLabelAttribute, getAnyContainingText, getInput} from "./xPath";
import {HOST} from "./constants";
import {Scope} from "./types";

export const allScopes: Scope[] = ["keys.edit", "translations.edit", "translations.view"];

export const createRepository = (name = "Repository", languages = [{name: "English", abbreviation: "en"}]) => {
    cy.visit(HOST + "/repositories");
    cy.wait(500);
    clickAdd();
    cy.xpath(getInput("name")).type(name);
    languages.forEach((language, index) => {
        cy.xpath(getInput(`languages.${index}.name`)).type(language.name);
        cy.xpath(getInput(`languages.${index}.abbreviation`)).type(language.abbreviation);
        if (index !== languages.length - 1) {

        }
    })
    cy.xpath(getAnyContainingText("SAVE")).click();
};

export const deleteRepository = (name = "Repository", force: boolean) => {
    cy.visit(HOST + "/repositories");
    cy.wait(1000)
    cy.contains("Repositories").should("be.visible");
    cy.xpath(getAnyContainingText(name)).click({force});
    cy.wait(100);
    cy.xpath(getAnyContainingText("Repository settings")).click({force});
    cy.xpath(getAnyContainingText("Delete repository")).click({force});
    const label = cy.xpath(getAnyContainingText("Rewrite text:")+"/ancestor::*[1]//input");
    label.type(name.toUpperCase(), {force});
    cy.xpath(getAnyContainingText("CONFIRM")).click({force});
};

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