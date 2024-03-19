import {
  commentsButton,
  createComment,
  deleteComment,
  resolveComment,
  unresolveComment,
} from '../../common/comments';
import { enterProject, visitList } from '../../common/projects';
import { waitForGlobalLoading } from '../../common/loading';
import { commentsTestData } from '../../common/apiCalls/testData/testData';
import { login } from '../../common/apiCalls/common';

describe('Translation comments', () => {
  beforeEach(() => {
    commentsTestData.clean();
    commentsTestData.generate();
    waitForGlobalLoading();
  });

  it("won't fail when translation is empty", () => {
    logInAs('franta');
    createComment('Cool comment 1', 'B key', 'en');
  });

  it('franta can add comment (manage)', () => {
    logInAs('franta');
    createComment('Cool comment 1', 'A key', 'en');
  });

  it('franta can delete all comments (manage)', () => {
    logInAs('franta');
    userCanDeleteComment('C key', 'en', 'First comment');
    userCanDeleteComment('C key', 'en', 'Second comment');
  });

  it('pepa can load more comments (edit)', () => {
    logInAs('pepa');
    commentsButton('D key', 'en').click();

    cy.gcy('comment-text').contains('comment 1').should('not.exist');
    cy.gcy('translations-comments-load-more-button').scrollIntoView().click();
    waitForGlobalLoading();
    cy.gcy('translations-comments-load-more-button').scrollIntoView().click();
    waitForGlobalLoading();
    cy.gcy('comment-text').contains('comment 11').should('exist');
  });

  it('jindra can add comment (translate)', () => {
    logInAs('jindra');
    createComment('Cool comment 1', 'A key', 'en');
  });

  it('jindra can get to edit mode (translate)', () => {
    logInAs('jindra');
    commentsButton('A key', 'en').click();
    cy.gcy('global-editor').should('be.visible');
  });

  it('jindra can delete only his comments (translate)', () => {
    logInAs('jindra');
    userCanDeleteComment('C key', 'en', 'First comment');
    userCantOpenMenu('C key', 'en', 'Second comment');
  });

  it('jindra can resolve comment (translate)', () => {
    logInAs('jindra');
    userCanResolveComment('C key', 'en', 'First comment');
    userCanResolveComment('C key', 'en', 'Second comment');
  });

  it('jindra can unresolve comment (translate)', () => {
    logInAs('jindra');
    userCanResolveComment('C key', 'en', 'First comment');
    userCanUnresolveComment('C key', 'en', 'First comment');
  });

  it('jindra can resolve comment (translate cs)', () => {
    logInAs('jindra');
    userCanResolveComment('A key', 'cs', 'First comment');
    userCanUnresolveComment('A key', 'cs', 'First comment');
  });

  it('jindra is not able to get to edit mode (translate cs)', () => {
    logInAs('jindra');
    commentsButton('A key', 'cs').click();
    cy.gcy('global-editor').should('not.exist');
  });

  it('vojta is not able to add comment (view)', () => {
    logInAs('vojta');
    commentsButton('A key', 'en').click();
    cy.gcy('translations-comments-input').should('not.exist');
  });

  it('vojta is not able to get to edit mode (view)', () => {
    logInAs('vojta');
    commentsButton('A key', 'en').click();
    cy.gcy('global-editor').should('not.exist');
  });

  it('vojta cant delete any comments (view)', () => {
    logInAs('vojta');
    userCantOpenMenu('C key', 'en', 'First comment');
    userCantOpenMenu('C key', 'en', 'Second comment');
  });

  it('vojta cant resolve any comments (view)', () => {
    logInAs('vojta');
    userCantResolveComment('C key', 'en', 'First comment');
    userCantResolveComment('C key', 'en', 'Second comment');
  });
});

function logInAs(user: string) {
  login(user, 'admin');
  visitList();
  enterProject("Franta's project", 'franta');
  cy.waitForDom();
}

function userCanResolveComment(key: string, lang: string, comment: string) {
  commentsButton(key, lang).click();
  resolveComment(comment);
  cy.gcy('translations-cell-cancel-button').click();
}

function userCantResolveComment(key: string, lang: string, comment: string) {
  commentsButton(key, lang).click();
  cy.gcy('comment-text')
    .contains(comment)
    .closestDcy('comment')
    .findDcy('comment-resolve')
    .should('not.exist');
  cy.gcy('translations-cell-cancel-button').click();
}

function userCanDeleteComment(key: string, lang: string, comment: string) {
  commentsButton(key, lang).click();
  deleteComment(comment);
  cy.contains(comment).should('not.exist');
  cy.gcy('translations-cell-cancel-button').click();
}

function userCanUnresolveComment(key: string, lang: string, comment: string) {
  commentsButton(key, lang).click();
  unresolveComment(comment);
  cy.gcy('translations-cell-cancel-button').click();
}

function userCantOpenMenu(key: string, lang: string, comment: string) {
  commentsButton(key, lang).click();
  cy.gcy('comment-text')
    .contains(comment)
    .closestDcy('comment')
    .findDcy('comment-menu')
    .should('not.exist');
  cy.gcy('translations-cell-cancel-button').click();
}
