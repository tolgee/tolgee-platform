import { waitForGlobalLoading } from './loading';
import { confirmStandard, gcy, gcyAdvanced } from './shared';

export function commentsButton(key: string, language: string) {
  return gcyAdvanced({ value: 'translations-table-cell', language, key })
    .trigger('mouseover')
    .findDcy('translations-cell-comments-button');
}

export function getCommentInputField() {
  return gcy('translations-comments-input-field');
}

export function createComment(text: string, key: string, lang: string) {
  commentsButton(key, lang).click();
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

export function mentionPickerItem(userId: number) {
  return gcyAdvanced({
    value: 'comment-mention-picker-item',
    'user-id': String(userId),
  });
}

export function commentMention(userId: number) {
  return gcyAdvanced({ value: 'comment-mention', 'user-id': String(userId) });
}
