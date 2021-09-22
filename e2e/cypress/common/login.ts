import { HOST } from '../common/constants';

export const loginWithFakeGithub = () => {
  cy.intercept('https://github.com/login/oauth/**', {
    statusCode: 200,
    body: 'Fake GitHub',
  });
  cy.contains('GitHub login').click();
  cy.contains('Fake GitHub').should('be.visible');
  cy.visit(
    HOST +
      '/login/auth_callback/github?scope=user%3Aemail&code=this_is_dummy_code'
  );
  cy.contains('Projects').should('be.visible');
};
