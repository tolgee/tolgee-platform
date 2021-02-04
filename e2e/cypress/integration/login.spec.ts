/// <reference types="cypress" />
import {HOST, PASSWORD, USERNAME} from "../fixtures/constants";
import {getAnyContainingText} from "../fixtures/xPath";
import {login} from "../fixtures/apiCalls";
import {getPopover} from "../fixtures/shared";

context('Login', () => {
    beforeEach(() => {
        cy.visit(HOST);
    });

    it('Will login', () => {
        cy.xpath('//input[@name="username"]')
            .type(USERNAME).should('have.value', USERNAME);
        cy.xpath('//input[@name="password"]')
            .type(PASSWORD).should('have.value', PASSWORD);
        cy.xpath("//button//*[text() = 'Login']").click();
        cy.xpath(getAnyContainingText("Login")).should("not.be.visible");
        cy.xpath("//*[@aria-controls='user-menu']").should("be.visible");
    });

    it('Will fail on invalid credentials', () => {
        cy.xpath('//input[@name="username"]')
            .type("aaaa").should('have.value', "aaaa");
        cy.xpath('//input[@name="password"]')
            .type(PASSWORD).should('have.value', PASSWORD);
        cy.xpath("//button//*[text() = 'Login']").click();
        cy.xpath(getAnyContainingText("Login")).should("be.visible");
        cy.contains("Invalid credentials").should("be.visible");
    });

    it('Will logout', () => {
        login().then(() => {
            cy.reload();
            cy.xpath("//*[@aria-controls='user-menu']").click();
            getPopover().contains("Logout").click();
            cy.contains("Login").should("be.visible");
        });
    });
});
