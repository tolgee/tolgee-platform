import { HOST } from '../../common/constants';
import { loginWithFake, loginWithFakeSso } from '../../common/login';
import { assertMessage, gcyAdvanced } from '../../common/shared';
import { authProviderChange } from '../../common/apiCalls/testData/testData';
import {
  login,
  enableOrganizationsSsoProvider,
  disableOrganizationsSsoProvider,
  enableGlobalSsoProvider,
  disableGlobalSsoProvider,
  setBypassSeatCountCheck,
} from '../../common/apiCalls/common';

describe('Authentication Provider Change', () => {
  beforeEach(() => {
    setBypassSeatCountCheck(true);
    authProviderChange.clean();
    authProviderChange.generate();
  });

  afterEach(() => {
    authProviderChange.clean();
    setBypassSeatCountCheck(false);
  });

  const testCombinations: [
    string,
    string,
    ('github' | 'google' | 'oauth2' | 'sso' | null)[]
  ][] = [
    [
      'User with no existing provider (native auth)',
      'userNoProvider@domain.com',
      ['github', 'google', 'oauth2', 'sso'],
    ],
    [
      'User with an existing third-party provider (e.g. GitHub)',
      'userGithub@domain.com',
      ['google', 'oauth2', 'sso', null],
    ],
  ];

  testCombinations.forEach(([description, user, providers]) => {
    context(description, () => {
      beforeEach(() => {
        enableGlobalSsoProvider();
        login(user);
        cy.visit(HOST + '/account/security');
      });

      afterEach(() => {
        disableGlobalSsoProvider();
      });

      providers.forEach((newProvider) => {
        function triggerAuthenticationProviderChange() {
          if (newProvider === null) {
            // Removing current provider
            return gcyAdvanced({
              value: 'account-security-provider-disconnect',
              provider: 'github',
            }).click();
          }

          // Simulate login with the provider
          return loginWithFake(
            newProvider,
            user,
            'account-security-provider-connect'
          );
        }
        it(`should handle switching to ${newProvider} provider`, () => {
          triggerAuthenticationProviderChange();
          cy.gcy('accept-auth-provider-change-info-text').should('be.visible');
          cy.gcy('accept-auth-provider-change-accept').click();

          assertMessage('Authentication provider changed sucessfully');
          cy.gcy('accept-auth-provider-change-info-text').should('not.exist');

          cy.url().should('eq', HOST + '/account/security');
        });

        it(`should handle rejecting switch to ${newProvider} provider`, () => {
          triggerAuthenticationProviderChange();
          cy.gcy('accept-auth-provider-change-info-text').should('be.visible');
          cy.gcy('accept-auth-provider-change-decline').click();

          assertMessage('Authentication provider change rejected');
          cy.gcy('accept-auth-provider-change-info-text').should('not.exist');

          cy.url().should('eq', HOST + '/account/security');
        });
      });
    });
  });

  describe('Forced SSO Migration', () => {
    beforeEach(() => {
      enableOrganizationsSsoProvider();
      login('userNoProviderForcedSsoOrganization@org-forced.com');
    });

    after(() => {
      disableOrganizationsSsoProvider();
    });

    it('redirects user to provider switch dialog on homepage and does not redirect away', () => {
      cy.visit(HOST);
      cy.gcy('sso-migration-info-text').should('be.visible');
      cy.wait(100);
      cy.gcy('sso-migration-info-text').should('be.visible');
    });

    it('completes SSO provider switch flow and auto-accepts final confirmation', () => {
      cy.visit(HOST);
      cy.gcy('sso-migration-info-text').should('be.visible');

      loginWithFakeSso(
        'userNoProviderForcedSsoOrganization@org-forced.com',
        'account-security-provider-connect'
      );

      assertMessage('Authentication provider changed sucessfully');
      cy.gcy('accept-auth-provider-change-info-text').should('not.exist');

      cy.visit(HOST);
      cy.contains('Projects').should('be.visible');
    });

    it('allows access to profile and security settings pages during SSO migration', () => {
      cy.visit(HOST + '/account/profile');
      cy.contains('User profile').should('be.visible');
      cy.wait(100);
      cy.url().should('eq', HOST + '/account/profile');
      cy.visit(HOST + '/account/security');
      cy.contains('Account security').should('be.visible');
      cy.wait(100);
      cy.url().should('eq', HOST + '/account/security');
    });
  });
});
