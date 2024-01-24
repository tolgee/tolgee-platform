import { createUser, deleteUserSql } from '../common/apiCalls/common';
import 'cypress-file-upload';
import { fillAndSubmitSignUpForm, visitSignUp } from '../common/login';

describe('User Limit', { retries: 5 }, () => {
  const generatedUserNames = [];

  beforeEach(() => {
    for (let i = 1; i <= 11; i++) {
      generatedUserNames.push(`user${i}@user.com`);
    }
  });

  afterEach(() => {
    generatedUserNames.forEach((username) => {
      deleteUserSql(username);
    });
  });

  it('throws when over limit', () => {
    visitSignUp();
    generatedUserNames.forEach((username, index) => {
      if (index >= 10) {
        return;
      }
      createUser(username, 'password', 'user');
    });
    fillAndSubmitSignUpForm(generatedUserNames[10], true);
    cy.contains('exceeded').should('be.visible');
    cy.contains('seats').should('be.visible');
  });
});
