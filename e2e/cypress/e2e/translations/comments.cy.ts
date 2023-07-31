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
    createComment('Cool comment 1', 1, 'en');
  });

  it('franta can add comment (manage)', () => {
    logInAs('franta');
    createComment('Cool comment 1', 0, 'en');
  });

  it('franta can delete all comments (manage)', () => {
    logInAs('franta');
    userCanDeleteComment(2, 'en', 'First comment');
    userCanDeleteComment(2, 'en', 'Second comment');
  });

  it('pepa can load more comments (edit)', () => {
    logInAs('pepa');
    commentsButton(3, 'en').click();

    cy.gcy('comment-text').contains('comment 1').should('not.exist');
    cy.gcy('translations-comments-load-more-button').scrollIntoView().click();
    waitForGlobalLoading();
    cy.gcy('comment-text').contains('comment 1').should('exist');
  });

  it('jindra can add comment (translate)', () => {
    logInAs('jindra');
    createComment('Cool comment 1', 0, 'en');
  });

  it('jindra can get to edit mode (translate)', () => {
    logInAs('jindra');
    commentsButton(0, 'en').click();
    cy.gcy('translations-cell-tab-edit').should('be.visible').click();
    cy.gcy('global-editor').should('be.visible');
  });

  it('jindra can delete only his comments (translate)', () => {
    logInAs('jindra');
    userCanDeleteComment(2, 'en', 'First comment');
    userCantOpenMenu(2, 'en', 'Second comment');
  });

  it('jindra can resolve comment (translate)', () => {
    logInAs('jindra');
    userCanResolveComment(2, 'en', 'First comment');
    userCanResolveComment(2, 'en', 'Second comment');
  });

  it('jindra can unresolve comment (translate)', () => {
    logInAs('jindra');
    userCanResolveComment(2, 'en', 'First comment');
    userCanUnresolveComment(2, 'en', 'First comment');
  });

  it('jindra can resolve comment (translate cs)', () => {
    logInAs('jindra');
    userCanResolveComment(0, 'cs', 'First comment');
    userCanUnresolveComment(0, 'cs', 'First comment');
  });

  it('jindra is not able to get to edit mode (translate cs)', () => {
    logInAs('jindra');
    commentsButton(0, 'cs').click();
    cy.gcy('translations-cell-tab-edit').should('not.exist');
  });

  it('vojta is not able to add comment (view)', () => {
    logInAs('vojta');
    commentsButton(0, 'en').click();
    cy.gcy('translations-comments-input').should('not.exist');
  });

  it('vojta is not able to get to edit mode (view)', () => {
    logInAs('vojta');
    commentsButton(0, 'en').click();
    cy.gcy('translations-cell-tab-edit').should('not.exist');
  });

  it('vojta cant delete any comments (view)', () => {
    logInAs('vojta');
    userCantOpenMenu(2, 'en', 'First comment');
    userCantOpenMenu(2, 'en', 'Second comment');
  });

  it('vojta cant resolve any comments (view)', () => {
    logInAs('vojta');
    userCantResolveComment(2, 'en', 'First comment');
    userCantResolveComment(2, 'en', 'Second comment');
  });
});

function logInAs(user: string) {
  login(user, 'admin');
  visitList();
  enterProject("Franta's project", 'franta');
  cy.waitForDom();
}

function userCanResolveComment(index: number, lang: string, comment: string) {
  commentsButton(index, lang).click();
  resolveComment(comment);
  cy.gcy('translations-cell-close').click();
}

function userCantResolveComment(index: number, lang: string, comment: string) {
  commentsButton(index, lang).click();
  cy.gcy('comment-text')
    .contains(comment)
    .closestDcy('comment')
    .findDcy('comment-resolve')
    .should('not.exist');
  cy.gcy('translations-cell-close').click();
}

function userCanDeleteComment(index: number, lang: string, comment: string) {
  commentsButton(index, lang).click();
  deleteComment(comment);
  cy.contains(comment).should('not.exist');
  cy.gcy('translations-cell-close').click();
}

function userCanUnresolveComment(index: number, lang: string, comment: string) {
  commentsButton(index, lang).click();
  unresolveComment(comment);
  cy.gcy('translations-cell-close').click();
}

function userCantOpenMenu(index: number, lang: string, comment: string) {
  commentsButton(index, lang).click();
  cy.gcy('comment-text')
    .contains(comment)
    .closestDcy('comment')
    .findDcy('comment-menu')
    .should('not.exist');
  cy.gcy('translations-cell-close').click();
}
