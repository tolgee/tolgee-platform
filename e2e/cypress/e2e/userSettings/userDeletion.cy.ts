import { HOST } from '../../common/constants';
import { userDeletionTestData } from '../../common/apiCalls/testData/testData';
import { assertMessage, confirmHardMode, gcy } from '../../common/shared';
import { login } from '../../common/apiCalls/common';
import { loginViaForm } from '../../common/login';

describe('User deletion', () => {
  beforeEach(() => {
    userDeletionTestData.clean();
    userDeletionTestData.generate();
  });

  afterEach(() => {
    userDeletionTestData.clean();
  });

  it('deletes Olga', () => {
    testDeleteUser({ username: 'olga', organizationsToContain: ['Olga'] });
  });

  it('deletes Pepa', () => {
    testDeleteUser({ username: 'pepa', organizationsToContain: [] });
  });

  it('deletes Pepa (only shared organization owner)', () => {
    testDeleteUser({ username: 'franta', organizationsToContain: ['Franta'] });
    testDeleteUser({
      username: 'pepa',
      organizationsToContain: ["Pepa's and Franta's org"],
    });
  });
});

function testDeleteUser({
  username,
  organizationsToContain,
}: {
  username: string;
  organizationsToContain: string[];
}) {
  login(username);
  visit();
  gcy('delete-user-button').click();
  if (organizationsToContain.length > 0) {
    gcy('user-delete-organization-message-item')
      .should('be.visible')
      .should('have.length', organizationsToContain.length);
    organizationsToContain.forEach((organizationName) => {
      gcy('user-delete-organization-message-item').should(
        'contain',
        organizationName
      );
    });
  }
  confirmHardMode();
  assertMessage('Account deleted');
  loginViaForm(username);
  cy.contains('Invalid credentials').should('be.visible');
}

function visit() {
  cy.visit(HOST + '/account/profile');
}
