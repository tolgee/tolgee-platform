import {cleanOrganizationData, cleanRepositoriesData, createOrganizationData, createRepositoriesData, login} from "../../fixtures/apiCalls";
import {HOST} from "../../fixtures/constants";
import 'cypress-file-upload';
import {assertMessage, clickGlobalSave, confirmHardMode, confirmStandard, gcy, goToPage, selectInRepositoryMenu} from "../../fixtures/shared";
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

        it("Has manage permissions on facebook (organization owner)", () => {
            validateManagePermissions("Facebook")
        })

        it.only("Has edit permissions on microsoft word (organization base)", () => {
            validateEditPermissions("Microsoft Word")
        })

        it("Has manage permissions (direct manage permissions)", () => {
            validateManagePermissions("Vaclav's funny repository")
        })

        it("Has view permissions on facebook (direct view permissions)", () => {
            validateViewPermissions("Vaclav's cool repository")
        })
    })

    describe("Vaclav's permissions", () => {
        before(() => {
            cleanRepositoriesData()
            createRepositoriesData()
        })

        beforeEach(() => {
            login("vaclav.novak@fake.com", "admin")
        })

        it("Has edit permission on excel (direct) ", () => {
            validateEditPermissions("Microsoft Excel")
        })

        it("Has translate permission on Powerpoint (direct) ", () => {
            validateTranslatePermissions("Microsoft Powerpoint")
        })
    })

    describe("Permission settings", () => {
        describe("Not modifying", () => {
            before(() => {
                cleanRepositoriesData()
                createRepositoriesData()
            })

            beforeEach(() => {
                login("cukrberg@facebook.com", "admin")
            })

            it("Can search in permissions", () => {
                visitList()
                enterRepositorySettings("Facebook itself")
                selectInRepositoryMenu("Permissions")
                gcy("global-list-search").find("input").type("Doe")
                gcy("global-paginated-list").within(() => {
                    gcy("global-list-item").should("have.length", 1).should("contain", "John Doe")
                })

            })

            it("Can paginate", () => {
                visitList()
                login("gates@microsoft.com", "admin")
                enterRepositorySettings("Microsoft Word")
                selectInRepositoryMenu("Permissions")
                goToPage(2)
                cy.contains("owner@zzzcool9.com (owner@zzzcool9.com)").should("be.visible")
            })

            it("Has enabled proper options for each user", () => {
                visitList()
                enterRepositorySettings("Facebook itself")
                selectInRepositoryMenu("Permissions")
                gcy("global-paginated-list").within(() => {
                    gcy("global-list-item").contains("John Doe").closest("li").within(() => {
                        gcy("permissions-revoke-button").should("be.disabled")
                        gcy("permissions-menu-button").should("be.enabled")
                    })
                    gcy("global-list-item").contains("Cukrberg").closest("li").within(() => {
                        gcy("permissions-revoke-button").should("be.disabled")
                        gcy("permissions-menu-button").should("be.disabled")
                    })
                })
            })
        })

        describe("Modifying", () => {
            beforeEach(() => {
                cleanRepositoriesData()
                createRepositoriesData()
                login("cukrberg@facebook.com", "admin")
            })

            it("Can modify permissions", () => {
                visitList()
                enterRepositorySettings("Facebook itself")
                selectInRepositoryMenu("Permissions")
                gcy("global-paginated-list").within(() => {
                    gcy("global-list-item").contains("Vaclav Novak").closest("li").within(() => {
                        gcy("permissions-menu-button").click()
                    })
                })
                gcy("permissions-menu").filter(":visible").contains("Manage").click()
                confirmStandard()
                login("vaclav.novak@fake.com", "admin")
                visitList()
                validateManagePermissions("Facebook itself")
            })


            it("Can revoke permissions", () => {
                visitList()
                enterRepositorySettings("Facebook itself")
                selectInRepositoryMenu("Permissions")
                gcy("global-paginated-list").within(() => {
                    gcy("global-list-item").contains("Vaclav Novak").closest("li").within(() => {
                        gcy("permissions-revoke-button").click()
                    })
                })
                confirmStandard()
                login("vaclav.novak@fake.com", "admin")
                visitList()
                cy.contains("Facebook itself").should("not.exist")
            })
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
    gcy("global-base-view-title").contains("Translations").should("be.visible")
}

const visitList = () => {
    cy.visit(`${HOST}`)
}

const MANAGE_REPOSITORY_ITEMS = ["Permissions", "Languages"]
const OTHER_REPOSITORY_ITEMS = ["Repositories", "Export"]

const assertManageMenuItemsNotVisible = () => {
    MANAGE_REPOSITORY_ITEMS.forEach(item => {
        gcy("repository-menu-items").should("not.contain", item)
    })
}

const assertOtherMenuItemsVisible = () => {
    OTHER_REPOSITORY_ITEMS.forEach(item => {
        gcy("repository-menu-items").should("contain", item)
    })
}

const validateManagePermissions = (repositoryName: string) => {
    visitList()
    enterRepositorySettings(repositoryName)
    cy.gcy("global-form-save-button").click()
    assertMessage("Repository settings are successfully saved.")
    enterRepository(repositoryName)
    MANAGE_REPOSITORY_ITEMS.forEach(item => {
        gcy("repository-menu-items").should("contain", item)
    })
    assertOtherMenuItemsVisible()
}

const validateEditPermissions = (repositoryName: string) => {
    visitList()
    enterRepository(repositoryName)
    selectInRepositoryMenu("Translations")
    assertManageMenuItemsNotVisible()
    assertOtherMenuItemsVisible()
    gcy("global-plus-button").should("be.visible").click()
    gcy("translations-add-key-field").find("textarea").filter(":visible").type("test");
    gcy("global-form-save-button").should("be.visible").click()
    assertMessage("Translation created")
    gcy("translations-row-checkbox").click()
    gcy("translations-delete-button").click()
    confirmStandard()
    assertMessage("Translations deleted!")
}

const validateTranslatePermissions = (repositoryName: string) => {
    visitList()
    enterRepository(repositoryName)
    selectInRepositoryMenu("Translations")
    assertManageMenuItemsNotVisible()
    assertOtherMenuItemsVisible()
    gcy("global-plus-button").should("not.exist")
    gcy("translations-row-checkbox").should("not.exist")
    gcy("translations-editable-cell").contains("This is test text!").should("be.visible").click()
    gcy("translations-editable-cell-editing").should("be.visible")
}


const validateViewPermissions = (repositoryName: string) => {
    visitList()
    enterRepository(repositoryName)
    gcy("repository-menu-items").should("contain", "Repositories")
    gcy("repository-menu-items").should("contain", "Export")
    assertManageMenuItemsNotVisible()
    assertOtherMenuItemsVisible()
    selectInRepositoryMenu("Translations")
    gcy("global-plus-button").should("not.exist")
}
