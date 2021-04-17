import {
    createUser,
    deleteAllEmails,
    deleteUserWithEmailVerification,
    disableEmailVerification,
    enableEmailVerification,
    getParsedEmailVerification,
    login
} from "../../fixtures/apiCalls";
import {HOST} from "../../fixtures/constants";
import {assertMessage} from "../../fixtures/shared";

describe('User profile', () => {
    const INITIAL_USERNAME = "honza@honza.com"
    const INITIAL_PASSWORD = "honza"
    const EMAIL_VERIFICATION_TEXT = "When you change your email, new e-mail will be set after its verification";

    function visit() {
        return cy.visit(HOST + "/user")
    }

    let NEW_EMAIL = "pavel@honza.com";
    beforeEach(() => {
        enableEmailVerification()
        deleteUserWithEmailVerification(INITIAL_USERNAME)
        deleteUserWithEmailVerification(NEW_EMAIL)
        createUser(INITIAL_USERNAME, INITIAL_PASSWORD)
        login(INITIAL_USERNAME, INITIAL_PASSWORD)
        deleteAllEmails()
        visit();
    })

    afterEach(() => {
        enableEmailVerification()
    })

    it("email verification on user update works", () => {
        cy.get("form").findInputByName("email").clear().type(NEW_EMAIL)
        cy.contains(EMAIL_VERIFICATION_TEXT).should("be.visible")
        cy.gcy("global-form-save-button").click()
        cy.contains("E-mail waiting for verification: pavel@honza.com").should("be.visible")
        getParsedEmailVerification().then(v => {
            cy.visit(v.verifyEmailLink)
            assertMessage("E-mail was verified.")
            visit()
            cy.contains("E-mail waiting for verification: pavel@honza.com").should("not.exist")
            cy.get("form").findInputByName("email").should("have.value", NEW_EMAIL)
        })
    })

    it.only("works without verification enabled", () => {
        disableEmailVerification()
        cy.get("form").findInputByName("email").clear().type(NEW_EMAIL)
        cy.contains(EMAIL_VERIFICATION_TEXT).should("not.exist")
        cy.gcy("global-form-save-button").click()
        cy.get("form").findInputByName("email").should("have.value", NEW_EMAIL)
    })
});
