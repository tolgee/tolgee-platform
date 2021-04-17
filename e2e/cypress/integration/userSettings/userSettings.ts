/// <reference types="cypress" />
import {getAnyContainingText} from "../../fixtures/xPath";
import {HOST} from "../../fixtures/constants";
import {createTestRepository, createUser, deleteUser, login} from "../../fixtures/apiCalls";
import {getPopover} from "../../fixtures/shared";

describe('User settings', () => {
    beforeEach(() => {
        login();
        cy.visit(HOST);
    });

    it('Will access api keys', () => {
        cy.xpath("//*[@aria-controls='user-menu']").click();
        cy.xpath(getAnyContainingText("Api keys")).click();
    });

    it('will open user menu from repositories', () => {
        createTestRepository().then(r => {
            cy.visit(`${HOST}/repositories/${r.body.id}`);
            cy.xpath("//*[@aria-controls='user-menu']").click();
            getPopover().contains("settings").click();
            cy.contains("User profile").should("be.visible");
        });
    });

    it('will access user settings', () => {
        cy.xpath("//*[@aria-controls='user-menu']").click();
        cy.get("#user-menu").contains("Account settings").click();
    });
});
