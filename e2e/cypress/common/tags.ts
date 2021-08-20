import { waitForGlobalLoading } from './loading';

export function getAddTagButton(index = 0) {
  return cy
    .gcy('translations-row')
    .eq(index)
    .trigger('mouseover')
    .findDcy('translations-tag-add');
}

export function createTag(name: string) {
  getAddTagButton().click();
  cy.focused().type(name);
  cy.gcy('tag-autocomplete-option').contains('Add').click();
  waitForGlobalLoading();
}
