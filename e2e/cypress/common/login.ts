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

export const loginWithFakeOAuth2 = () => {
  cy.intercept('https://dummy-url.com/**', {
    statusCode: 200,
    body: 'Fake OAuht2',
  }).as('oauth2');
  cy.contains('OAuth2 login').click();

  cy.wait('@oauth2').then((interception) => {
    const params = new URL(interception.request.url).searchParams;
    expect(params.get('client_id')).to.eq('dummy_client_id');
    expect(params.get('response_type')).to.eq('code');
    expect(params.get('scope')).to.eq('openid email profile');
  });

  cy.contains('Fake OAuht2').should('be.visible');
  cy.visit(
    HOST +
      '/login/auth_callback/oauth2?code=this_is_dummy_code&redirect_uri=https%3A%2F%2Fdummy-url.com%2Fcallback'
  );
  cy.contains('Projects').should('be.visible');
};
