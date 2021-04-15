import {cleanOrganizationData, cleanRepositoriesData, createOrganizationData, createRepositoriesData, login} from "../../fixtures/apiCalls";
import {HOST} from "../../fixtures/constants";
import 'cypress-file-upload';
import {assertMessage, clickGlobalSave, confirmHardMode, confirmStandard, gcy, goToPage, selectInRepositoryMenu} from "../../fixtures/shared";
import {RepositoryDTO} from "../../../../webapp/src/service/response.types";

describe('Repositories Basics', () => {
    beforeEach(() => {
        cleanRepositoriesData()
        createRepositoriesData()
        login("cukrberg@facebook.com", "admin")
        cy.visit(`${HOST}`)
    })

    it("Searches in list", () => {
        gcy("global-list-search").find("input").type("Facebook")
        gcy("global-paginated-list").within(() => gcy("global-list-item").should("have.length", 1)).contains("Facebook itself")
    })

    it("Creates repository with user owner", () => {
        createRepository("I am a great repository", "Mark Cukrberg")
    })

    it("Creates with organization owner", () => {
        createRepository("I am a great repository", "Facebook")
    })

    const createRepository = (name: string, owner: string) => {
        gcy("global-plus-button").click()
        gcy("repository-owner-select").click()
        gcy("repository-owner-select-item").contains(owner).click()
        gcy("repository-name-field").find("input").type(name)
        gcy("repository-language-name-field").find("input").type("English")
        gcy("repository-language-abbreviation-field").find("input").type("en")
        gcy("global-field-array-plus-button").click()
        gcy("repository-language-name-field").eq(1).find("input").type("Deutsch")
        gcy("repository-language-abbreviation-field").eq(1).find("input").type("de")
        gcy("global-field-array-plus-button").click()
        gcy("repository-language-name-field").eq(2).find("input").type("Česky")
        gcy("repository-language-abbreviation-field").eq(2).find("input").type("cs")
        gcy("global-form-save-button").click()
        assertMessage("Repository created")
        gcy("global-paginated-list").contains(name).closest("li").within(() => {
            gcy("repository-list-owner").contains(owner).should("be.visible")
        })
    }

    after(() => {
    })
})
