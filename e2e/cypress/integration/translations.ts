import {clickAdd, getPopover} from "../common/shared";
import {getAnyContainingAriaLabelAttribute, getAnyContainingText, getClosestContainingText} from "../common/xPath";
import {createProject, deleteProject, login, setTranslations} from "../common/apiCalls";
import {HOST} from "../common/constants";
import {ProjectDTO} from "../../../webapp/src/service/response.types";

describe('Translations', () => {
    let project: ProjectDTO = null

    beforeEach(() => {
        cy.wrap(null).then(() => login().then(() => {
            cy.wrap(null).then(() => createProject({
                    name: "Test",
                    languages: [
                        {
                            abbreviation: "en",
                            name: "English"
                        },
                        {
                            abbreviation: "cs",
                            name: "Česky"
                        }
                    ]
                }
            ).then(r => {
                project = r.body as ProjectDTO;
                window.localStorage.setItem('selectedLanguages', `{"${project.id}":["en"]}`);
                visit();
            }))
        }));
    });

    afterEach(() => {
        cy.wrap(null).then(() => deleteProject(project.id));
    })

    it("will paginate", () => {
        const promises = []
        for (let i = 1; i < 21; i++) {
            promises.push(setTranslations(project.id, `Cool key ${i.toString().padStart(2, "0")}`, {"en": "Cool"}))
        }
        Cypress.Promise.all(promises).then(() => {
            setPerPage(20, 10)
            goToNextPage();
            cy.contains("key 11").should("be.visible")
            cy.contains("key 20").should("be.visible")
            goToPreviousPage();
            cy.contains("key 01").should("be.visible")
            cy.contains("key 10").should("be.visible")
            cy.contains("1-10 of 20").should("be.visible")
        })
    })

    it("will ask for confirmation on page change", () => {
        const promises = []
        for (let i = 1; i < 15; i++) {
            promises.push(setTranslations(project.id, `Cool key ${i.toString().padStart(2, "0")}`, {"en": "Cool"}))
        }
        Cypress.Promise.all(promises).then(() => {
            visit();
            setPerPage(20, 10)
            editKey("Cool key 01", "Cool key edited", false);
            goToNextPage();
            cy.contains(`Do you want to discard your change from "Cool key 01" to "Cool key edited"?`).should("be.visible");
            clickDiscardChanges()
            cy.contains("Cool key 14").xpath("./parent::*//button[@type='button']").should("be.visible");
        })
    })


    it('will create translation', () => {
        createTranslation("Test key", "Translated test key", {isFirst: true});
        cy.contains("Translation created").should("be.visible")
        cy.xpath(getAnyContainingText("Key", "a")).xpath(getClosestContainingText("Test key")).scrollIntoView().should("be.visible");
        cy.xpath(getAnyContainingText("Key", "a")).xpath(getClosestContainingText("Translated test key")).should("be.visible")
        createTranslation("Test key 2", "Translated test key 2", {isFirst: false});
        cy.xpath(getAnyContainingText("Key", "a")).xpath(getClosestContainingText("Test key 2")).scrollIntoView().should("be.visible");
        cy.xpath(getAnyContainingText("Key", "a")).xpath(getClosestContainingText("Translated test key 2")).should("be.visible")
    });


    describe("with 5 translations", () => {
        beforeEach(() => {
            const promises = []
            for (let i = 1; i < 5; i++) {
                promises.push(setTranslations(project.id, `Cool key ${i.toString().padStart(2, "0")}`, {
                    "en": `Cool translated text ${i}`,
                    "cs": `Studený přeložený text ${i}`
                }))
            }
            cy.wrap(null).then(() => Cypress.Promise.all(promises).then(() => {
                visit();
            }))
        })

        it("will edit key", () => {
            cy.contains("Cool key 01").xpath("./parent::div/button[@aria-label='edit']").click()
            cy.contains("Cool key 01").type("{backspace}{backspace}edited");
            cy.contains("Cool key edited").xpath("./parent::*//button[@type='submit']").click()
            cy.xpath(`${getAnyContainingText("Cool key edited")}/parent::*//button[@type='submit']`).should("not.exist");
            cy.contains("Cool key edited").should("be.visible");
            cy.contains("Cool key 02").should("be.visible");
            cy.contains("Cool key 04").should("be.visible");
        })

        it("will edit translation", () => {
            cy.contains("Cool translated text 1").last().xpath("./parent::div/button[@aria-label='edit']").click()
            cy.contains("Cool translated text 1").clear().type("Super cool changed text...");
            cy.contains("Super cool changed text...").xpath("./parent::*//button[@type='submit']").click()
            cy.xpath(`${getAnyContainingText("Super cool changed text...")}/parent::*//button[@type='submit']`).should("not.exist");
            cy.contains("Super cool changed text...").should("be.visible");
            cy.contains("Cool translated text 2").should("be.visible");
        })

        it("will cancel key edit", () => {
            cy.contains("Cool key 01").xpath("./parent::div/button[@aria-label='edit']").click();
            cy.contains("Cool key 01").type("{backspace}{backspace}edited");
            cy.contains("Cool key edited").xpath("./parent::*//button[@type='button']").click();
            cy.contains("Do you want to discard your change from").should("be.visible");
            clickDiscardChanges()
            cy.contains("Cool key edited").should("not.exist");
            cy.contains("Cool key 01").should("be.visible");
        })

        it("will ask for confirmation on changed edit", () => {
            editKey("Cool key 01", "Cool key edited", false);
            cy.contains("Cool key 04").xpath("./parent::div/button[@aria-label='edit']").click();
            cy.contains(`Do you want to discard your change from "Cool key 01" to "Cool key edited"?`).should("be.visible");
            clickDiscardChanges()
            cy.contains("Cool key 04").xpath("./parent::*//button[@type='button']").should("be.visible");
        })

        it("will ask for confirmation on per page change", () => {
            editKey("Cool key 01", "Cool key edited", false);
            setPerPage(20, 10);
            cy.contains(`Do you want to discard your change from "Cool key 01" to "Cool key edited"?`).should("be.visible");
            clickDiscardChanges()
            cy.contains("Cool key 04").xpath("./parent::*//button[@type='button']").should("be.visible");
        })

        describe("Options", () => {
            it("will select language", () => {
                const toggleLang = (lang) => {
                    cy.get("#languages-select-translations").click();
                    cy.get("#language-select-translations-menu").contains(lang).should("be.visible").click();
                    cy.get("body").click();
                    // wait for loading to disappear
                    cy.gcy('global-base-view-loading').should('not.exist');
                }
                toggleLang("Česky");
                cy.contains("Studený přeložený text 1").should("not.exist");
                toggleLang("Česky")
                cy.contains("Studený přeložený text 1").should("be.visible");
                toggleLang("Česky")
                cy.contains("Studený přeložený text 1").should("not.exist");
                toggleLang("English")
                cy.contains("Select at least one language").should("be.visible");
            })

            it("will search", () => {
                cy.get("#standard-search").type("Cool key 04")
                cy.contains("Cool key 01").should("not.exist")
                cy.contains("Cool key 04").should("be.visible")
            })

            it("will toggle key", () => {
                cy.contains("Cool key 01").should("be.visible")
                cy.contains("Show keys").click();
                cy.contains("Cool key 01").should("not.exist")
                cy.contains("Show keys").click();
                cy.contains("Cool key 01").should("be.visible")
            })
        })
    })

    const visit = () => {
        cy.visit(`${HOST}/projects/${project.id}/translations`)
    }
});

const editKey = (oldValue: string, newValue: string, save: boolean = true) => {
    cy.contains(oldValue).xpath("./parent::div/button[@aria-label='edit']").click();
    cy.contains("Cool key 01").clear().type(newValue);
    if (save) {
        cy.contains("Super cool changed text...").xpath("./parent::*//button[@type='submit']").click()
    }
}

function createTranslation(testKey: string, testTranslated: string, options: { isFirst?: boolean }) {
    if (options?.isFirst) {
        clickAdd();
    } else {
        cy.xpath(getAnyContainingText("Add", "span")).click()
    }
    cy.xpath("//textarea[@name='key']").type(testKey);
    cy.xpath("//textarea[@name='translations.en']").type(testTranslated);
    cy.xpath(getAnyContainingText("save")).click();
}

function setPerPage(current: number, newValue: number) {
    cy.xpath(getAnyContainingText("Per page:")).xpath(getClosestContainingText(current.toString())).click();
    getPopover().contains(newValue.toString()).click()
    // wait for loading to disappear
    cy.gcy('global-base-view-loading').should('not.exist');
}

function goToNextPage() {
    cy.xpath(getAnyContainingAriaLabelAttribute("Next page")).click()
}

function goToPreviousPage() {
    cy.xpath(getAnyContainingAriaLabelAttribute("Previous page")).click()
}

function clickDiscardChanges(){
    cy.xpath(getAnyContainingText("Discard changes", "button")).click();
}

