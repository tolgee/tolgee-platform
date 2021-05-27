import {cleanImportData, generateBaseImportData, login} from "../../common/apiCalls";
import 'cypress-file-upload';
import {gcy} from "../../common/shared";
import {getFileIssuesDialog, getLanguageRow, visitImport} from "../../common/import";

describe('Import Adding files', () => {
    beforeEach(() => {
        cleanImportData()

        generateBaseImportData().then(repository => {
            login("franta")
            visitImport(repository.body.id);
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

    after(() => {
        cleanImportData()
    })
})

