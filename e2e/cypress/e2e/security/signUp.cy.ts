import { HOST } from '../../common/constants';
import {
  createProject,
  deleteAllEmails,
  deleteUserSql,
  disableEmailVerification,
  disableRegistration,
  enableEmailVerification,
  enableRegistration,
  getParsedEmailVerification,
  getParsedEmailVerificationByIndex,
  getRecaptchaSiteKey,
  getUser,
  login,
  logout,
  releaseForcedDate,
  setProperty,
  setRecaptchaSecretKey,
  setRecaptchaSiteKey,
  v2apiFetch,
} from '../../common/apiCalls/common';
import { assertMessage, gcy } from '../../common/shared';
import {
  checkAnonymousIdSet,
  checkAnonymousIdUnset,
  checkAnonymousUserIdentified,
  fillAndSubmitSignUpForm,
  loginWithFakeGithub,
  signUpAfter,
  visitSignUp,
} from '../../common/login';
import { ProjectDTO } from '../../../../webapp/src/service/response.types';
import { waitForGlobalLoading } from '../../common/loading';

const TEST_USERNAME = 'johndoe@doe.com';

const createProjectWithInvitation = (name: string) => {
  return login().then(() =>
    createProject({
      name,
      languages: [
        {
          tag: 'en',
          name: 'English',
          originalName: 'English',
          flagEmoji: '🇬🇧',
        },
      ],
    }).then((projectResponse: { body: ProjectDTO }) => {
      return v2apiFetch(`projects/${projectResponse.body.id}/invite`, {
        method: 'PUT',
        body: { type: 'VIEW', name: 'Franta' },
      }).then((invitation) => {
        logout();
        return {
          projectId: projectResponse.body.id,
          invitationLink: `${HOST}/accept_invitation/${invitation.body.code}`,
        };
      });
    })
  );
};

context('Sign up', () => {
  let recaptchaSiteKey;

  beforeEach(() => {
    releaseForcedDate();
    getRecaptchaSiteKey().then((it) => (recaptchaSiteKey = it));
    logout();
    visitSignUp();
    deleteUserSql(TEST_USERNAME);
    deleteAllEmails();
    enableEmailVerification();
    enableRegistration();
  });

  afterEach(() => {
    signUpAfter(TEST_USERNAME);
  });
  describe('without recaptcha', () => {
    beforeEach(() => {
      setRecaptchaSiteKey(null);
    });

    it('Signs up without recaptcha', () => {
      checkAnonymousIdSet();
      visitSignUp();
      cy.intercept('/**/sign_up', (req) => {
        expect(req.body.recaptchaToken).be.undefined;
      }).as('signUp');
      fillAndSubmitSignUpForm(TEST_USERNAME);
      cy.wait(['@signUp']);
      cy.contains('Thank you for signing up!').should('be.visible');

      cy.contains('Check your inbox');
      setProperty('recaptcha.siteKey', recaptchaSiteKey);
    });
  });

  it('Fails on recaptcha', () => {
    setRecaptchaSecretKey('negative_dummy_secret_key');
    cy.intercept('/**/sign_up', (req) => {
      expect(req.body.recaptchaToken).have.length.greaterThan(10);
    }).as('signUp');
    fillAndSubmitSignUpForm(TEST_USERNAME);
    cy.wait(['@signUp']);
    setRecaptchaSecretKey('dummy_secret_key');
    cy.contains('You are a robot').should('be.visible');
  });

  it('Signs up', () => {
    checkAnonymousIdSet();
    cy.intercept('/**/sign_up', (req) => {
      expect(req.body.recaptchaToken).have.length.greaterThan(10);
    }).as('signUp');
    fillAndSubmitSignUpForm(TEST_USERNAME);
    cy.wait(['@signUp']);
    cy.contains('Thank you for signing up!').should('be.visible');

    cy.contains('Check your inbox');

    cy.wait(1000);
    getUser(TEST_USERNAME).then((u) => {
      expect(u[0]).be.equal(TEST_USERNAME);
      expect(u[1]).be.not.null;
    });
    getParsedEmailVerification().then((r) => {
      cy.wrap(r.fromAddress).should('contain', 'no-reply@tolgee.io');
      cy.wrap(r.toAddress).should('contain', TEST_USERNAME);
      cy.visit(r.verifyEmailLink);
      assertMessage('Email was verified');
    });
    checkAnonymousIdUnset();
    checkAnonymousUserIdentified();
  });

  it('Signs up and resend email verification', () => {
    fillAndSubmitSignUpForm(TEST_USERNAME);
    cy.contains('Thank you for signing up!').should('be.visible');

    cy.contains('Check your inbox');

    gcy('resend-email-button').click();
    cy.contains('Your verification link has been resent.');

    // Email sent after registration are no longer valid
    getParsedEmailVerificationByIndex(1).then((r) => {
      cy.wrap(r.fromAddress).should('contain', 'no-reply@tolgee.io');
      cy.wrap(r.toAddress).should('contain', TEST_USERNAME);
      cy.visit(r.verifyEmailLink);
      assertMessage('Validation code or link is invalid');
    });

    getParsedEmailVerificationByIndex(0).then((r) => {
      cy.wrap(r.fromAddress).should('contain', 'no-reply@tolgee.io');
      cy.wrap(r.toAddress).should('contain', TEST_USERNAME);
      cy.visit(r.verifyEmailLink);
      assertMessage('Email was verified');
    });
    cy.contains('Projects').should('be.visible');
  });

  it('Signs up without email verification', () => {
    disableEmailVerification();
    fillAndSubmitSignUpForm(TEST_USERNAME);
    assertMessage('Thank you for signing up!');
    cy.contains('Projects');
  });

  it('Signs up with project invitation code', () => {
    disableEmailVerification();
    createProjectWithInvitation('Test').then(({ invitationLink }) => {
      logout();
      cy.log(window.localStorage.getItem('jwtToken'));
      cy.visit(HOST + '/sign_up');
      fillAndSubmitSignUpForm(TEST_USERNAME);
      cy.contains('Projects').should('be.visible');
      cy.visit(invitationLink);
      cy.gcy('accept-invitation-info-text').should(
        'contain',
        'admin invited you to the project Test'
      );
      cy.gcy('accept-invitation-accept').should('be.visible').click();
      assertMessage('Invitation successfully accepted');
    });
  });

  it('Remembers code after sign up', () => {
    disableEmailVerification();
    createProjectWithInvitation('Crazy project').then(({ invitationLink }) => {
      cy.visit(invitationLink);
      cy.gcy('accept-invitation-accept').should('be.visible').click();
      cy.gcy('pending-invitation-banner').should('be.visible');
      cy.visit(HOST + '/sign_up');
      cy.gcy('pending-invitation-banner').should('be.visible');
      fillAndSubmitSignUpForm(TEST_USERNAME, false);
      assertMessage('Thank you for signing up!');
      cy.contains('Crazy project').should('be.visible');
    });
  });

  it('Invitation can be declined right away', () => {
    disableEmailVerification();
    createProjectWithInvitation('Crazy project').then(({ invitationLink }) => {
      cy.visit(invitationLink);
      cy.gcy('accept-invitation-decline').should('be.visible').click();
      cy.gcy('login-button').should('be.visible');
      cy.gcy('pending-invitation-banner').should('not.exist');
    });
  });

  it('Invitation can be declined later', () => {
    disableEmailVerification();
    createProjectWithInvitation('Crazy project').then(({ invitationLink }) => {
      cy.visit(invitationLink);
      cy.gcy('accept-invitation-accept').should('be.visible').click();
      cy.gcy('login-button').should('be.visible');
      cy.gcy('pending-invitation-dismiss').should('be.visible').click();
      cy.gcy('pending-invitation-banner').should('not.exist');
    });
  });

  it('Allows sign up when user has invitation', () => {
    disableEmailVerification();
    disableRegistration();
    createProjectWithInvitation('Crazy project').then(({ invitationLink }) => {
      cy.visit(invitationLink);
      cy.gcy('accept-invitation-accept').should('be.visible').click();
      cy.gcy('pending-invitation-banner').should('be.visible');
      cy.visit(HOST + '/sign_up');
      fillAndSubmitSignUpForm(TEST_USERNAME, false);
      assertMessage('Thank you for signing up!');
      cy.contains('Crazy project').should('be.visible');
    });
  });

  it('Works with github signup', () => {
    disableEmailVerification();
    createProjectWithInvitation('Crazy project').then(({ invitationLink }) => {
      cy.visit(HOST + '/login');
      loginWithFakeGithub(TEST_USERNAME);
      cy.contains('Projects').should('be.visible');
      cy.log(invitationLink);
      cy.visit(invitationLink);
      cy.waitForDom();
      waitForGlobalLoading();
      cy.contains('Crazy project').should('be.visible');
    });
  });

  it('Remember code after github signup', () => {
    disableEmailVerification();
    createProjectWithInvitation('Crazy project').then(({ invitationLink }) => {
      cy.visit(invitationLink);
      cy.gcy('accept-invitation-accept').click();
      cy.gcy('pending-invitation-banner').should('be.visible');
      cy.intercept('/api/public/authorize_oauth/github**').as('GithubSignup');
      loginWithFakeGithub(TEST_USERNAME);
      cy.contains('Projects').should('be.visible');
      cy.wait('@GithubSignup').then((interception) => {
        assert.isTrue(interception.request.url.includes('invitationCode'));
      });
    });
  });
});
