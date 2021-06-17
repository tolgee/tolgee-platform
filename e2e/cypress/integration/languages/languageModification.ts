import {
  cleanLanguagesData,
  generateLanguagesData,
  login,
} from "../../common/apiCalls";
import { setLanguageData, visitProjectSettings } from "../../common/languages";
import { assertMessage, gcy, selectInSelect } from "../../common/shared";

describe("Language modification", () => {
  beforeEach(() => {
    cleanLanguagesData();

    generateLanguagesData().then((languageData) => {
      login("franta");
      visitProjectSettings(languageData.body.id);
    });
  });

  it("modifies language", () => {
    editLanguage("English | English (en)");
    setLanguageData({
      name: "Modified",
      originalName: "Modified Original",
      tag: "tg",
      flagEmoji: "🇦🇿",
    });
    gcy("global-form-save-button").click();
    getLanguageListItem("Modified | Modified Original (tg)")
      .should("be.visible")
      .find("img")
      .should("have.attr", "alt", "🇦🇿");
  });

  it("cannot delete base language", () => {
    editLanguage("English | English (en)");
    gcy("language-delete-button").click();
    assertMessage("Cannot delete base language");
  });

  it("Sets project base language", () => {
    selectInSelect(gcy("base-language-select"), "German");
    gcy("global-form-save-button").click();
    cy.reload();
    gcy("base-language-select").should("contain", "German");
    gcy("base-language-select").find("img").should("have.attr", "alt", "🇩🇪");
  });
});

const editLanguage = (name: string) => {
  getLanguageListItem(name)
    .findDcy("project-settings-languages-list-edit-button")
    .click();
};

const getLanguageListItem = (name: string) => {
  return gcy("global-paginated-list")
    .contains(name)
    .closestDcy("project-settings-languages-list-item")
    .parent();
};
