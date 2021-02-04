/// <reference types="cypress" />
import {getAnyContainingText} from "../fixtures/xPath";
import {HOST} from "../fixtures/constants";
import {createTestRepository, createUser, deleteUser, login} from "../fixtures/apiCalls";
import {getPopover} from "../fixtures/shared";

describe('User settings', () => {
    beforeEach(() => {
        login();
        cy.visit(HOST);
    });

    it('Will access api keys', () => {
        cy.xpath("//*[@aria-controls='user-menu']").click();
        cy.xpath(getAnyContainingText("Api keys")).click();
    });

    it('will access user settings', () => {
        cy.xpath("//*[@aria-controls='user-menu']").click();
        cy.xpath(getAnyContainingText("settings")).click();
    });

    it('will open user menu from repositories', () => {
        createTestRepository().then(r => {
            cy.visit(`${HOST}/repositories/${r.body.id}`);
            cy.xpath("//*[@aria-controls='user-menu']").click();
            getPopover().contains("Settings").click();
            cy.contains("User account settings").should("be.visible");
        });
    });

    describe("settings", () => {
        it("will change user settings", () => {
            createUser().then(() => {
                deleteUser("test@mail.com").then(() => {
                    login("test", "test").then(() => {
                        cy.visit(`${HOST}/user`);
                        cy.contains("User account settings").should("be.visible");
                        cy.xpath("//*[@name='name']").clear().type("New name")
                        cy.xpath("//*[@name='email']").clear().type("test@mail.com");
                        cy.contains("Save").click();
                        cy.contains("User data updated").should("be.visible")
                        cy.reload();
                        cy.contains("New name").should("be.visible");
                        cy.xpath("//*[@name='email']").should("have.value", "test@mail.com");
                    })
                });
            })
        })

        it.only("will fail when user exists", () => {
            createUser().then(() => {
                createUser("test@mail.com").then(() => {
                    login("test", "test").then(() => {
                        cy.visit(`${HOST}/user`);
                        cy.contains("User account settings").should("be.visible");
                        cy.xpath("//*[@name='name']").clear().type("New name")
                        cy.xpath("//*[@name='email']").clear().type("test@mail.com");
                        cy.contains("Save").click();
                        cy.contains("User name already exists.").should("be.visible");
                        cy.reload();
                        cy.xpath("//*[@name='email']").should("have.value", "test");
                    })
                });
            })
        })
    })


});
