import {
    cleanImportData,
    generateAllSelectedImportData,
    generateApplicableImportData,
    generateBaseImportData,
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

        it.only("Adds new language", () => {
            const filename = "multilang.json (en)"
            let select = getLanguageSelect(filename)
            selectInSelect(select, "Add new")
            cy.xpath("//input[@name='name']").type("New language")
            cy.xpath("//input[@name='abbreviation']").type("nl")
            gcy("global-form-save-button").click()
            getLanguageSelect(filename).should("contain.text", "New language")
            selectInRepositoryMenu("Languages")
            cy.contains("New language").should("be.visible")
        })

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

        describe("Resolving", () => {
            it("shows correct initial data", () => {
                getLanguageRow("multilang.json (en)").findDcy("import-result-resolve-button").click()
                gcy("import-resolution-dialog-resolved-count").should("have.text", "0")
                gcy("import-resolution-dialog-conflict-count").should("have.text", "4")
                gcy("import-resolution-dialog-show-resolved-switch").find("input").should("be.checked")
                gcy("import-resolution-dialog-data-row").should("have.length", 4)
                gcy("import-resolution-dialog-data-row").should("contain.text", "What a text")
            })

            it("resolves row (one by one)", () => {
                getLanguageRow("multilang.json (en)").findDcy("import-result-resolve-button").click()
                gcy("import-resolution-dialog-data-row").contains("Overridden").click()
                cy.xpath("//*[@data-cy-selected]").should("have.length", 1)
                findResolutionRow("what a key").findDcy("import-resolution-dialog-existing-translation")
                    .should("not.have.attr", "data-cy-selected")
                findResolutionRow("what a key").findDcy("import-resolution-dialog-new-translation")
                    .should("have.attr", "data-cy-selected")

                findResolutionRow("what a nice key").contains("What a text").click()
                cy.xpath("//*[@data-cy-selected]").should("have.length", 2)
                findResolutionRow("what a nice key").findDcy("import-resolution-dialog-new-translation")
                    .should("not.have.attr", "data-cy-selected")
                findResolutionRow("what a nice key").findDcy("import-resolution-dialog-existing-translation")
                    .should("have.attr", "data-cy-selected")

                gcy("import-resolution-dialog-resolved-count").should("have.text", "2")
            })

            it("accept all new", () => {
                getLanguageRow("multilang.json (en)").findDcy("import-result-resolve-button").click()
                gcy("import-resolution-dialog-accept-imported-button").click()
                cy.xpath("//*[@data-cy-selected]").should("have.length", 4)
                gcy("import-resolution-dialog-new-translation").each(($el) => {
                    cy.wrap($el).should("have.attr", "data-cy-selected")
                })
                gcy("import-resolution-dialog-resolved-count").should("have.text", "4")
            })

            it("accept all old", () => {
                getLanguageRow("multilang.json (en)").findDcy("import-result-resolve-button").click()
                gcy("import-resolution-dialog-accept-old-button").click()
                cy.xpath("//*[@data-cy-selected]").should("have.length", 4)
                gcy("import-resolution-dialog-existing-translation").each(($el) => {
                    cy.wrap($el).should("have.attr", "data-cy-selected")
                })
                gcy("import-resolution-dialog-resolved-count").should("have.text", "4")
            })
        })
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
                toggleInMultiselect(gcy("translations-language-select-form-control"), ["French", "English"])
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

        it("Paginates in resolution dialog", () => {
                getLanguageRow("another.json (fr)").findDcy("import-result-resolve-button").click()
                getResolutionDialog().contains("Resolve conflicts").should("be.visible")
                assertInResolutionDialog("this_is_key_1")
                assertInResolutionDialog("I am translation 1")
                assertInResolutionDialog("I am import translation 1")
                contextGoToPage(getResolutionDialog(), 6)
                assertInResolutionDialog("I am import translation 300")
            }
        )
    })

    describe("file types", () => {
        beforeEach(() => {
            generateBaseImportData().then(repository => {
                login("franta")
                visit(repository.body.id);
            })
        })


        it("uploads .po", () => {
            cy.get("[data-cy=dropzone]")
                .attachFile("import/po/example.po", {subjectType: 'drag-n-drop'})
            gcy("import-result-total-count-cell").should("contain.text", "8")
        })

        it("uploads multiple xliffs", () => {
            cy.get("[data-cy=dropzone]")
                .attachFile(
                    ["import/xliff/larger.xlf", "import/xliff/example.xliff", "import/xliff/error_example.xliff"],
                    {subjectType: 'drag-n-drop'})

            gcy("import-result-total-count-cell", {timeout: 10000}).should("exist")
            getLanguageRow("larger.xlf (en)").should("contain.text", "1151")
            getLanguageRow("larger.xlf (cs)").should("contain.text", "1151")
            getLanguageRow("example.xliff (en)").findDcy("import-result-total-count-cell").should("contain.text", "176")
        })

        it("has valid xliff errors", () => {
            cy.get("[data-cy=dropzone]")
                .attachFile(
                    "import/xliff/error_example.xliff",
                    {subjectType: 'drag-n-drop'})

            gcy("import-result-file-cell").findDcy("import-result-file-warnings").should("contain.text", "4")
            gcy("import-result-file-cell").findDcy("import-file-issues-button").click()
            getFileIssuesDialog().contains("Target translation not provided (key name: vpn.main.back)").should("be.visible")
            getFileIssuesDialog().contains("Translation id attribute not provided " +
                "(File original: ../src/platforms/android/androidauthenticationview.qml)")
                .should("be.visible")
        })
    })

    after(() => {
        //  cleanImportData()
    })

    const visit = (repositoryId: number) => {
        cy.visit(`${HOST}/repositories/${repositoryId}/import`)
    }

    const getLanguageRow = (filename: string) => {
        return cy.xpath(`//*[@data-cy='import-result-row']//*[. = '${filename}']`).closestDcy("import-result-row")
    }

    const getLanguageSelect = (filename: string) => {
        return cy.xpath(`//*[@data-cy='import-result-row']//*[. = '${filename}']` +
            `/ancestor::*[@data-cy='import-result-row']`
            + `//*[@data-cy='import-row-language-select-form-control']`)
    }

    const getFileIssuesDialog = () => {
        return gcy("import-file-issues-dialog")
    }

    const getShowDataDialog = () => {
        return gcy("import-show-data-dialog")
    }

    const getResolutionDialog = () => {
        return gcy("import-conflict-resolution-dialog")
    }

    const assertInResultDialog = (text: string) => {
        getShowDataDialog().contains(text).scrollIntoView().should("be.visible")
    }

    const assertInResolutionDialog = (text: string) => {
        getResolutionDialog().contains(text).scrollIntoView().should("be.visible")
    }

    const findResolutionRow = (key: string) => {
        return gcy("import-resolution-dialog-data-row")
            .findDcy("import-resolution-dialog-key-name")
            .contains(key)
            .closestDcy("import-resolution-dialog-data-row")
    }
})

