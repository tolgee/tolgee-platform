import { waitForGlobalLoading } from './loading';
import { confirmStandard } from './shared';

export function commentsButton(index: number, language: string) {
  return cy
    .gcy('translations-row')
    .eq(index)
    .trigger('mouseover')
    .findDcy('translations-table-cell-language')
    .contains(language)
    .closestDcy('translations-table-cell')
    .findDcy('translations-cell-comments-button');
}

export function createComment(text: string, index: number, lang: string) {
  commentsButton(index, lang).click();
  waitForGlobalLoading();
  cy.gcy('translations-comments-input').type(text).type('{enter}');
  waitForGlobalLoading();
  cy.gcy('comment-text').contains(text).should('be.visible');
}

export function deleteComment(text: string) {
  cy.gcy('comment-text')
    .contains(text)
    .closestDcy('comment')
    .findDcy('comment-menu')
    .click();
  cy.gcy('comment-menu-delete').click();
  confirmStandard();
  waitForGlobalLoading();
  cy.contains(text).should('not.exist');
}

export function unresolveComment(text: string) {
  cy.gcy('comment-text')
    .contains(text)
    .closestDcy('comment')
    .findDcy('comment-menu')
    .click();
  cy.gcy('comment-menu-needs-resolution').click();
  waitForGlobalLoading();
  cy.gcy('comment-text')
    .contains(text)
    .closestDcy('comment')
    .findDcy('comment-resolve')
    .should('exist');
}

export function resolveComment(text: string) {
  cy.gcy('comment-text')
    .contains(text)
    .closestDcy('comment')
    .findDcy('comment-resolve')
    .click();
  waitForGlobalLoading();
  cy.gcy('comment-text')
    .contains(text)
    .closestDcy('comment')
    .findDcy('comment-resolve')
    .should('not.exist');
}
