import {HOST} from "../common/constants";
import {getInput} from "../common/xPath";
import {
    deleteAllEmails,
    deleteUserWithEmailVerification,
    disableEmailVerification,
    enableEmailVerification,
    getParsedEmailVerification,
    getUser
} from "../common/apiCalls";
import {assertMessage} from "../common/shared";


const TEST_USERNAME = "test@tolgee.io";
context('Login', () => {
    beforeEach(() => {
        cy.visit(HOST + "/sign_up");
        deleteUserWithEmailVerification(TEST_USERNAME)
        deleteAllEmails()
        enableEmailVerification()
    });

    afterEach(() => {
        //enableEmailVerification()
        deleteUserWithEmailVerification(TEST_USERNAME)
    })

    it('Will sign up', () => {
        fillAndSubmitForm()
        cy.contains("Thank you for signing up. To verify your e-mail please follow instructions sent to provided e-mail address.").should("be.visible")
        getUser(TEST_USERNAME).then(u => {
            expect(u[0]).be.equal(TEST_USERNAME)
            expect(u[1]).be.not.null
        })
        getParsedEmailVerification().then(r => {
            cy.wrap(r.fromAddress).should("contain", "no-reply@tolgee.io")
            cy.wrap(r.toAddress).should("contain", TEST_USERNAME)
            cy.visit(r.verifyEmailLink)
            assertMessage("E-mail was verified.")
        })
    });

    it("will sign up without email verification", () => {
        disableEmailVerification()
        fillAndSubmitForm()
        assertMessage("Thanks for your sign up!")
        cy.gcy("global-base-view-title").contains("Projects")
    })
});

const fillAndSubmitForm = () => {
    cy.xpath(getInput("name")).type("Test user");
    cy.xpath(getInput("email")).type(TEST_USERNAME);
    cy.xpath(getInput("password")).type("password");
    cy.xpath(getInput("passwordRepeat")).type("password");
    cy.contains("Submit").click()
}
