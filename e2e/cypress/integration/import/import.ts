import {
    cleanImportData,
    generateAllSelectedImportData,
    generateApplicableImportData,
    generateImportData,
    generateLotOfImportData,
    login
} from "../../common/apiCalls";
import {HOST} from "../../common/constants";
import 'cypress-file-upload';
import {confirmStandard, contextGoToPage, gcy, selectInRepositoryMenu, selectInSelect, toggleInMultiselect} from "../../common/shared";

describe('Import', () => {
    beforeEach(() => {
        cleanImportData()
    })

    describe("With basic data", () => {
        beforeEach(() => {
            generateImportData().then(importData => {
                login("franta")
                visit(importData.body.repository.id);
            })
        })

        it("Shows correct import result", () => {
            cy.gcy("import-result-row").should("have.length", 3)


            getLanguageRow("multilang.json (en)").within(() => {
                    cy.gcy("import-result-resolve-button")
                        .should("contain", "0 / 4").should("not.be.disabled");
                    cy.gcy("import-result-language-menu-cell").should("not.contain", "German")
                    cy.gcy("import-result-language-menu-cell").should("contain", "English")
                    cy.gcy("import-result-total-count-cell").should("contain", "6")

                }
            )

            getLanguageRow("multilang.json (de)").within(() => {
                    cy.gcy("import-result-resolve-button")
                        .should("contain", "0 / 0").should("be.disabled");
                    cy.gcy("import-result-language-menu-cell").should("contain", "German")
                    cy.gcy("import-result-language-menu-cell").should("not.contain", "English")
                }
            )
        })

        it("Shows correct file issues number", () => {
                getLanguageRow("multilang.json (en)").findDcy("import-result-file-warnings").should("contain", "4")
            }
        )

        it("Selects language", () => {
                const select = getLanguageSelect("multilang.json (fr)")
                selectInSelect(select, "French")
                select.should("contain", "French")
                select.should("not.contain", "Czech")
            }
        )

        it("Changes language", () => {
                const filename = "multilang.json (en)"
                let select = getLanguageSelect(filename)
                selectInSelect(select, "French")
                select.should("contain", "French")
                select.should("not.contain", "Czech")
                cy.reload()
                getLanguageSelect(filename)
                select.should("contain", "French")
            }
        )

        it("Clears existing language", () => {
                const filename = "multilang.json (en)"
                getLanguageRow(filename).should("contain.text", "English")
                getLanguageRow(filename).findDcy("import-row-language-select-clear-button").click().should("not.exist")
                getLanguageSelect(filename).should("not.contain", "English")
                cy.reload()
                getLanguageSelect(filename)
                getLanguageSelect(filename).should("not.contain.text", "English")
                getLanguageRow(filename).gcy("import-result-resolved-conflicts-cell").should("contain.text", "0 / 0")
            }
        )

        it("Deletes language", () => {
                getLanguageRow("multilang.json (en)").findDcy("import-result-delete-language-button").click()
                confirmStandard()
                cy.reload()
                cy.gcy("import-result-row").should("have.length", 2)
                cy.reload()
                cy.gcy("import-result-row").should("have.length", 2)
            }
        )

        it("Cancels import", () => {
                gcy("import_cancel_import_button").click()
                confirmStandard()
                cy.gcy("import-result-row").should("not.exist")
            }
        )

        it("Doesn't apply when language not selected", () => {
                const filename = "multilang.json (fr)"
                getLanguageSelect(filename).should("not.contain.text", "Select existing language")
                gcy("import_apply_import_button").click()
                getLanguageSelect(filename).should("contain.text", "Select existing language")
            }
        )
    })

    describe("All selected", () => {
        beforeEach(() => {
            generateAllSelectedImportData().then(importData => {
                login("franta")
                visit(importData.body.repository.id);
            })
        })

        it("Does not apply when row not resolved", () => {
                gcy("import_apply_import_button").click()
                gcy("import-conflicts-not-resolved-dialog").should("contain", "Conflicts not resolved")
                gcy("import-conflicts-not-resolved-dialog-resolve-button").click()
                gcy("import-conflict-resolution-dialog").should("be.visible").should("contain.text", "Resolve conflicts")
            }
        )
    })

    describe("With applicable data", () => {
        beforeEach(() => {
            generateApplicableImportData().then(importData => {
                login("franta")
                visit(importData.body.repository.id);
            })
        })

        it("Applies import", () => {
                gcy("import_apply_import_button").click()
                cy.gcy("import-result-row").should("not.exist")
                selectInRepositoryMenu("Translations")
                toggleInMultiselect(gcy("translations-language-select-form-control"), ["French"])
                cy.gcy("translations-editable-cell").contains("What a text").should("be.visible")
                cy.gcy("translations-editable-cell").contains("What a french text").should("be.visible")
            }
        )
    })

    describe("With lot of data", () => {
        beforeEach(() => {
            generateLotOfImportData().then(importData => {
                login("franta")
                visit(importData.body.repository.id);
            })
        })

        it("Shows correct file issues", () => {
                getLanguageRow("multilang.json (en)").findDcy("import-file-issues-button").click()
                getFileIssuesDialog().contains("File issues").should("be.visible")
                getFileIssuesDialog().contains("Key is empty (key index: 1)").should("be.visible")
                getFileIssuesDialog().contains("Key is not string (key name: 4, key index: 2)").should("be.visible")
                getFileIssuesDialog().contains("Value is empty (key name: value_is_emtpy_key)").should("be.visible")
                getFileIssuesDialog().contains("Value is not string (key name: value_is_not_string_key, key index: 5, value: 1)")
                    .should("be.visible")
                getFileIssuesDialog().contains("Key is empty (key index: 12)").scrollIntoView().should("be.visible")
                contextGoToPage(getFileIssuesDialog(), 2)
                getFileIssuesDialog().contains("Key is empty (key index: 32)").scrollIntoView().should("be.visible")
            }
        )

        it("Shows correct result", () => {
                getLanguageRow("another.json (fr)").findDcy("import-result-show-all-translations-button").click()
                getShowDataDialog().should("be.visible")
                assertInResultDialog("this_is_key_1")
                assertInResultDialog("I am import translation 1")
                assertInResultDialog("this_is_key_50")
                contextGoToPage(getShowDataDialog(), 6)
                assertInResultDialog("this_is_key_300")
            }
        )

        it("Searches", () => {
                getLanguageRow("another.json (fr)").findDcy("import-result-show-all-translations-button").click()
                gcy("global-search-field").filter(":visible").find("input").type("this_is_key_145")
                assertInResultDialog("this_is_key_145")
                assertInResultDialog("I am import translation 145")
            }
        )
    })

    after(() => {
        //  cleanImportData()
    })

    const visit = (repositoryId: number) => {
        cy.visit(`${HOST}/repositories/${repositoryId}/import`)
    }

    const getLanguageRow = (filename: string) => {
        return cy.gcy("import-result-row").contains(filename).closestDcy("import-result-row")
    }

    const getLanguageSelect = (filename: string) => {
        return getLanguageRow(filename).findDcy("import-row-language-select-form-control")
    }

    const getFileIssuesDialog = () => {
        return gcy("import-file-issues-dialog")
    }

    const getShowDataDialog = () => {
        return gcy("import-show-data-dialog")
    }

    const assertInResultDialog = (text: string) => {
        getShowDataDialog().contains(text).scrollIntoView().should("be.visible")

    }
})

