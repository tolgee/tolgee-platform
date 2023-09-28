import { confirmHardMode, gcy, gcyAdvanced, getInputByName } from './shared';

export const selectFlag = (emoji: string) => {
  gcy('languages-flag-selector-open-button').click();
  cy.xpath(`//img[@alt='${emoji}']`).click();
};

export const setLanguageData = (data: {
  name: string;
  originalName: string;
  tag: string;
  flagEmoji: string;
}) => {
  getCustomNameInput().clear().type(data.name);
  getInputByName('originalName').clear().type(data.originalName);
  getInputByName('tag').clear().type(data.tag);
  selectFlag(data.flagEmoji);
};

export const getCustomNameInput = () =>
  gcy('language-modify-form').xpath(".//input[@name='name']");

export const typeToAutocomplete = (text: string) => {
  gcy('languages-create-autocomplete-field').find('input').type(text);
};

export const selectInAutocomplete = (containedText: string) => {
  return gcy('languages-create-autocomplete-suggested-option')
    .contains(containedText)
    .click();
};

export const visitLanguageSettings = (langTag: string) => {
  gcyAdvanced({
    value: 'project-settings-languages-list-edit-button',
    language: langTag,
  }).click();
};

export const deleteLanguage = () => {
  gcy('language-delete-button').click();
  confirmHardMode();
};
