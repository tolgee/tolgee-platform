import {HOST} from "../fixtures/constants";
import {getInput} from "../fixtures/xPath";
import {deleteUserWithInvitationCode, getUser} from "../fixtures/apiCalls";


const TEST_USERNAME = "test@polygloat.io";
context('Login', () => {
    beforeEach(() => {
        cy.visit(HOST + "/sign_up");
        deleteUserWithInvitationCode(TEST_USERNAME)
    });

    afterEach(() => {
        deleteUserWithInvitationCode(TEST_USERNAME)
    })

    it.only('Will sign up', () => {
        cy.xpath(getInput("name")).type("Test user");
        cy.xpath(getInput("email")).type(TEST_USERNAME);
        cy.xpath(getInput("password")).type("password");
        cy.xpath(getInput("passwordRepeat")).type("password");
        cy.contains("Submit").click()
        cy.contains("Thank you for signing up. To verify your e-mail please follow instructions sent to provided e-mail address.").should("be.visible")
        getUser(TEST_USERNAME).then(u => {
            expect(u[0]).be.equal(TEST_USERNAME)
            expect(u[1]).be.not.null
        })
    });
});
