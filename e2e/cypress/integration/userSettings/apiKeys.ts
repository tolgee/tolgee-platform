import {allScopes, assertMessage, clickAdd, getPopover} from "../../fixtures/shared";
import {getAnyContainingText, getClosestContainingText} from "../../fixtures/xPath";
import {HOST} from "../../fixtures/constants";
import {cleanRepositoriesData, createApiKey, createRepositoriesData, createRepository, deleteRepository, login} from "../../fixtures/apiCalls";
import {Scope} from "../../fixtures/types";
import {ApiKeyDTO} from "../../../../webapp/src/service/response.types";

describe('Api keys', () => {
    let repository;


    describe("As admin", () => {
        beforeEach(() => {
            cy.wrap(null).then(() => login().then(() => {
                createRepository({
                    name: "Test", languages: [{abbreviation: "en", name: "English"}]
                }).then(r => repository = r.body);
            }));
            cy.visit(HOST + '/apiKeys');
        });

        afterEach(() => {
            cy.wrap(null).then(() => deleteRepository(repository.id));
        })

        it('Will add an api key', () => {
            create("Test", ["translations.view", "translations.edit"]);
            cy.contains("keys.edit").should("not.exist");
            cy.contains("translations.view").should("be.visible");
            cy.contains("translations.edit").should("be.visible");
        });


        it('Will delete an api key', () => {
            createApiKey({
                repositoryId: repository.id,
                scopes: ["keys.edit", "keys.edit", "translations.view"]
            }).then((key: ApiKeyDTO) => {
                cy.reload()
                cy.contains("API key:").should("be.visible")
                del(key.key);
                cy.contains("API key successfully deleted!").should("be.visible")
            });
        });

        it('Will edit an api key', () => {
            createApiKey({
                repositoryId: repository.id,
                scopes: ["keys.edit", "keys.edit", "translations.view"]
            }).then((key: ApiKeyDTO) => {
                cy.reload()
                cy.contains("API key:").should("be.visible")
                cy.gcy("api-keys-edit-button").eq(1).click()
                cy.gcy("api-keys-create-edit-dialog").contains("translations.edit").click()
                cy.gcy("api-keys-create-edit-dialog").contains("keys.edit").click()
                cy.gcy("global-form-save-button").click()
                assertMessage("API key successfully edited!")
            });
        });
    })

    it("will create API Key for user with lower permissions", () => {
        cleanRepositoriesData()
        createRepositoriesData()
        login("cukrberg@facebook.com", "admin")
        visit()
        clickAdd()
        cy.gcy("global-form-select").contains("Facebook itself").click()
        cy.gcy("api-keys-repository-select-item").contains("Vaclav's cool repository").click()
        cy.gcy("global-form-save-button").click()
        assertMessage("API key successfully created")
    })
});

const visit = () => {
    cy.visit(HOST + '/apiKeys');
}

const create = (repository: string, scopes: Scope[]) => {
    clickAdd();
    cy.contains("Generate api key").xpath(getClosestContainingText("Application")).click();
    getPopover().contains(repository).click();
    const toRemove = new Set(allScopes);
    scopes.forEach(s => toRemove.delete(s));
    toRemove.forEach(s => cy.contains("Generate api key").xpath(getClosestContainingText(s)).click())
    cy.xpath(getAnyContainingText("generate", "button")).click();
};

const del = (key) => {
    cy.wait(500);
    cy.xpath(getAnyContainingText(key))
        .last()
        .xpath("(./ancestor::*//*[@aria-label='delete'])[1]")
        .scrollIntoView({offset: {top: -500, left: 0}}).click();
    cy.xpath(getAnyContainingText("confirm", "span")).click();
};
