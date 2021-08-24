import {
  cleanCommentsData,
  generateCommentsData,
  login,
} from '../../common/apiCalls';
import {
  commentsButton,
  createComment,
  deleteComment,
} from '../../common/comments';
import { enterProject } from '../../common/projects';
import { waitForGlobalLoading } from '../../common/loading';

describe('Translation comments', () => {
  beforeEach(() => {
    cleanCommentsData();
    generateCommentsData();
    waitForGlobalLoading();
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

  it('pepa can add comment (edit)', () => {
    logInAs('pepa');
    createComment('Cool comment 1', 0, 'en');
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
    userCantDeleteComment(2, 'en', 'Second comment');
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
    userCantDeleteComment(2, 'en', 'First comment');
    userCantDeleteComment(2, 'en', 'Second comment');
  });
});

function logInAs(user: string) {
  login(user, 'admin');
  enterProject("Franta's project");
}

function userCanDeleteComment(index: number, lang: string, comment: string) {
  commentsButton(index, lang).click();
  deleteComment(comment);
  cy.contains(comment).should('not.exist');
  cy.gcy('translations-cell-close').click();
}

function userCantDeleteComment(index: number, lang: string, comment: string) {
  commentsButton(index, lang).click();
  cy.gcy('comment-text')
    .contains(comment)
    .closestDcy('comment')
    .findDcy('comment-menu')
    .should('not.exist');
  cy.gcy('translations-cell-close').click();
}
