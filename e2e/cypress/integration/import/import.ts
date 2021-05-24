import {cleanImportData, generateImportData, login} from "../../common/apiCalls";
import {HOST} from "../../common/constants";
import 'cypress-file-upload';
import {confirmStandard, gcy, selectInSelect} from "../../common/shared";

describe('Import', () => {
    beforeEach(() => {
        cleanImportData()
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

    it.only("Applies import", () => {
            gcy("import_apply_import_button").click()
        }
    )


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
})

