import {cleanOrganizationData, createOrganizationData, login} from "../../fixtures/apiCalls";
import {HOST} from "../../fixtures/constants";
import 'cypress-file-upload';
import {assertMessage, confirmStandard, gcy, goToPage} from "../../fixtures/shared";

describe('Organization Members', () => {
    beforeEach(async () => {
        await login().promisify()
        await cleanOrganizationData().promisify()
        await createOrganizationData().promisify()
        await visit();
    })

    it("contains organization users", () => {
        gcy("global-paginated-list").within(() => {
            cy.contains("Cukrberg").closestDataCy("global-list-item").contains("cukrberg@facebook.com").should("be.visible")
            cy.contains("admin")
            cy.contains("Goldberg")
            cy.contains("Bill Gates")
        })
    })

    it("May change role to member to other owner", () => {
        setGoldbergMember()
    })

    it("Can remove other users", () => {
        gcy("global-paginated-list").within(() => {
            cy.contains("Goldberg").closestDataCy("global-list-item").within(() => {
                cy.gcy("organization-members-remove-user-button").click()
            })
        })
        confirmStandard()
        assertMessage("User removed from organization")
        gcy("global-paginated-list").within(() => {
            cy.contains("Cukrberg").closestDataCy("global-list-item").within(() => {
                cy.gcy("organization-members-remove-user-button").click()
            })
        })
        confirmStandard()
        assertMessage("User removed from organization")
    })

    it("Can leave", () => {
        leaveOrganization();
        assertMessage("Organization left")
    })

    it("Cannot leave when single owner", () => {
        setGoldbergMember()
        leaveOrganization()
        assertMessage("Organization has no other owner.")
    })

    it("Can search", () => {
        cy.gcy("global-paginated-list").within(() => {
            cy.contains("Cukrberg").should("exist")
        })

        cy.gcy("global-list-search").within(() => {
            cy.get("input").type("Bill")
        })

        cy.gcy("global-paginated-list").within(() => {
            cy.gcy("global-list-item").within(() => {
                cy.contains("Cukrberg").should("not.exist")
                cy.contains("Bill Gates").should("be.visible")
            })
        })
    })

    it.only("Paginates", () => {
        cy.visit(`${HOST}/organizations/facebook/members`)
        gcy("global-paginated-list").contains("Cukrberg").should("be.visible")
        gcy("global-paginated-list").contains("owner@zzzcool16.com").should("be.visible")
        goToPage(2)
        gcy("global-paginated-list").contains("owner@zzzcool17.com").should("be.visible")
    })


    after(() => {
        //cleanOrganizationData()
    })

    const visit = async () => {
        await cy.visit(`${HOST}/organizations/tolgee/members`).promisify()
    }

    const setGoldbergMember = () => {
        gcy("global-paginated-list").within(() => {
            cy.contains("Goldberg").closestDataCy("global-list-item").within(() => {
                cy.gcy("organization-role-menu-button").click()
            })
        })
        cy.gcy("organization-role-menu").filter(':visible').within(() => {
            cy.contains("MEMBER").click()
        })
        confirmStandard()
        assertMessage("Organization role changed")
    }

    function leaveOrganization() {
        gcy("global-paginated-list").within(() => {
            cy.contains("admin").closestDataCy("global-list-item").within(() => {
                cy.gcy("organization-members-leave-button").click()
            })
        })
        confirmStandard()
    }

})

