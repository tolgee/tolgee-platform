import {
  createUser,
  deleteUserSql,
  setBypassSeatCountCheck,
} from '../common/apiCalls/common';
import 'cypress-file-upload';
import { fillAndSubmitSignUpForm, visitSignUp } from '../common/login';
import { gcy } from '../common/shared';

describe('User Limit', { retries: 5 }, () => {
  const generatedUserNames = [];

  before(() => {
    setBypassSeatCountCheck(false);
  });

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
    gcy('signup-error-free-seat-limit').should('be.visible');
  });
});
