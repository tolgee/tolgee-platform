import {HOST} from "../../common/constants";
import 'cypress-file-upload';
import {createTestRepository, disableAuthentication, enableAuthentication, login} from "../../common/apiCalls";
import {gcy} from "../../common/shared";

describe('Test no authentication mode', () => {
    beforeEach(() => {
        enableAuthentication()
        login().then(() => {
            createTestRepository()
        })
        cy.visit(`${HOST}`)
    })

    it("Has API keys item in repository menu", () => {
        disableAuthentication()
        createTestRepository()
        cy.reload()
        gcy("repository-settings-button").click()
        gcy("repository-menu-items").should("contain", "API keys")
    })

    it("Has no user menu", () => {
        cy.reload()
        const globalUserMenuDataCy = "global-user-menu-button"
        gcy(globalUserMenuDataCy).should("be.visible")
        disableAuthentication()
        cy.reload()
        cy.contains("Repositories").should("be.visible")
        gcy(globalUserMenuDataCy).should("not.exist")
    })


    it("Has no side menu in api keys", () => {
        cy.visit(HOST + "/apiKeys")
        cy.contains("My API keys")
        const userAccountSideMenuDataCy = "user-account-side-menu";
        cy.gcy(userAccountSideMenuDataCy).should("be.visible")
        disableAuthentication()
        cy.reload()
        cy.contains("My API keys").should("be.visible")
        cy.gcy(userAccountSideMenuDataCy).should("not.exist")
    })

    it("Has menu under title in api keys", () => {
        cy.visit(HOST + "/apiKeys")
        cy.contains("My API keys")
        const dataCy = "user-organizations-settings-subtitle-link";
        gcy(dataCy).should("be.visible")
        disableAuthentication()
        cy.reload()
        cy.contains("My API keys").should("be.visible")
        cy.gcy(dataCy).should("not.exist")
    })

    after(() => {
        enableAuthentication()
    })
})
