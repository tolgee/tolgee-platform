import {cleanOrganizationData, createOrganizationData, login} from "../../fixtures/apiCalls";
import {HOST} from "../../fixtures/constants";
import 'cypress-file-upload';
import {assertMessage, clickGlobalSave, confirmHardMode, gcy} from "../../fixtures/shared";

describe('Organization Settings', () => {
    beforeEach(async () => {
        login()
        cleanOrganizationData()
        createOrganizationData()
        visitProfile()
    })

    const newValues = {
        name: "What a nice organization",
        addressPart: "what-a-nice-organization",
        description: "This is an nice updated value!"
    }


    it("modifies organization", () => {
        gcy("organization-name-field").within(() => cy.get("input").clear().type(newValues.name))
        gcy("organization-address-part-field").within(() => cy.get("input").should("have.value", newValues.addressPart))
        gcy("organization-description-field").within(() => cy.get("input").clear().type(newValues.description))
        clickGlobalSave()
        cy.contains("Organization settings updated").should("be.visible")
        cy.visit(`${HOST}/organizations/what-a-nice-organization/profile`)
        gcy("organization-name-field").within(() => cy.get("input").should("have.value", newValues.name))
        gcy("organization-description-field").within(() => cy.get("input").should("have.value", newValues.description))
    })

    it("changes member privileges", () => {
        gcy("organization-side-menu").contains("Member privileges").click()
        gcy("permissions-menu-button").click()
        gcy("permissions-menu").within(() => {
            cy.contains("Translate").click()
        })
        confirmHardMode()
        assertMessage("Privileges set")
        visitMemberPrivileges()
        gcy("permissions-menu-button").contains("Translate")
    })

    it("member privileges change doesn't affect profile", () => {
        gcy("organization-side-menu").contains("Member privileges").click()
        gcy("permissions-menu-button").click()
        gcy("permissions-menu").within(() => {
            cy.contains("Translate").click()
        })
        confirmHardMode()
        visitProfile()
        gcy("organization-name-field").within(() => cy.get("input").should("have.value", "Tolgee"))
        gcy("organization-address-part-field").within(() => cy.get("input").should("have.value", "tolgee"))
        gcy("organization-description-field").within(() => cy.get("input").should("have.value", "This is us"))
    })

    it("deletes organization", () => {
        gcy("organization-delete-button").click()
        confirmHardMode()
        assertMessage("Organization deleted")
    })


    after(() => {
        cleanOrganizationData()
    })

    const visitProfile = () => {
        cy.visit(`${HOST}/organizations/tolgee/profile`)
    }

    const visitMemberPrivileges = async () => {
        cy.visit(`${HOST}/organizations/tolgee/member-privileges`)
    }

})

