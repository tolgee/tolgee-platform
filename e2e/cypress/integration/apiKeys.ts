import {allScopes, clickAdd, getPopover} from "../fixtures/shared";
import {getAnyContainingText, getClosestContainingText} from "../fixtures/xPath";
import {HOST} from "../fixtures/constants";
import {createApiKey, createRepository, deleteRepository, login} from "../fixtures/apiCalls";
import {Scope} from "../fixtures/types";
import {ApiKeyDTO} from "../../../webapp/src/service/response.types";

describe('Api keys', () => {
    let repository;

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
        cy.contains("key.edit").should("not.be.visible");
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
            cy.contains("Api key successfully deleted!").should("be.visible")
        });
    });

    it('Will edit an api key', () => {
        createApiKey({
            repositoryId: repository.id,
            scopes: ["keys.edit", "keys.edit", "translations.view"]
        }).then((key: ApiKeyDTO) => {
            cy.reload()
            cy.contains("API key:").should("be.visible")
            del(key.key);
            cy.contains("Api key successfully deleted!").should("be.visible")
        });
    });
});


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

const edit = (key, scopes: Scope[]) => {
    cy.wait(500);
    cy.xpath(getAnyContainingText(key))
        .last()
        .xpath("(./ancestor::*//*[@aria-label='edit'])[1]")
        .scrollIntoView({offset: {top: -500, left: 0}}).click();
    getPopover().xpath("//input[@type='checkbox' and @checked]").each(c => cy.wrap(c).click());

    cy.xpath(getAnyContainingText("save")).click();
};