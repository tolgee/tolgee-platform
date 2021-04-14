import {cleanOrganizationData, cleanRepositoriesData, createOrganizationData, createRepositoriesData, login} from "../../fixtures/apiCalls";
import {HOST} from "../../fixtures/constants";
import 'cypress-file-upload';
import {assertMessage, clickGlobalSave, confirmHardMode, gcy, goToPage, selectInRepositoryMenu} from "../../fixtures/shared";
import {RepositoryDTO} from "../../../../webapp/src/service/response.types";

describe('Organization Settings', () => {
    beforeEach(() => {

    })

    describe("Cukrberg's permissions", () => {
        before(() => {
            cleanRepositoriesData()
            createRepositoriesData()
        })

        beforeEach(() => {
            login("cukrberg@facebook.com", "admin")
        })

        const validateManagePermissions = (repositoryName: string) => {
            visitList()
            enterRepositorySettings(repositoryName)
            cy.gcy("global-form-save-button").click()
            assertMessage("Repository settings are successfully saved.")
        }

        const validateEditPermissions = (repositoryName: string) => {
            visitList()
            enterRepository(repositoryName)
            selectInRepositoryMenu("Translations")
            gcy("global-plus-button").should("be.visible").click()

            gcy("translations-add-key-field").find("textarea").filter(":visible").type("test");
            gcy("global-form-save-button").should("be.visible").click()
            assertMessage("Translation created")
        }

        it("Has manage permissions on facebook (organization owner)", () => {
            validateManagePermissions("Facebook")
        })

        it("Has manage permissions on facebook (direct manage permissions)", () => {
            validateManagePermissions("Vaclav's funny repository")
        })

        it("Has edit permissions on microsoft word (organization base)", () => {
            validateEditPermissions("Microsoft Word")
        })
    })

    after(() => {
    })
})

const enterRepositorySettings = (repositoryName: string) => {
    visitList()

    gcy("global-paginated-list").contains(repositoryName).closest("li").within(() => {
        cy.gcy("repository-settings-button").should("be.visible").click()
    })
}

const enterRepository = (repositoryName: string) => {
    visitList()

    gcy("global-paginated-list").contains(repositoryName).closest("a").click()
}

const visitList = () => {
    cy.visit(`${HOST}`)
}

