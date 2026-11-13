import {
  createUser,
  deleteAllEmails,
  deleteUserSql,
  disableEmailVerification,
  enableEmailVerification,
  getParsedEmailVerification,
  login,
} from '../../common/apiCalls/common';
import { communityStatsData } from '../../common/apiCalls/testData/testData';
import { HOST } from '../../common/constants';
import { waitForGlobalLoading } from '../../common/loading';
import { assertMessage } from '../../common/shared';
import { E2UserProfile } from '../../compounds/userSettings/E2UserProfile';

const profile = new E2UserProfile();

describe('User profile', () => {
  const INITIAL_EMAIL = 'honza@honza.com';
  const INITIAL_PASSWORD = 'honzaaaaaaaa';
  const EMAIL_VERIFICATION_TEXT =
    'When you change your email, new email will be set after its verification';
  const NEW_EMAIL = 'pavel@honza.com';

  function visit() {
    cy.visit(HOST + '/account/profile');
  }

  beforeEach(() => {
    enableEmailVerification();
    createUser(INITIAL_EMAIL, INITIAL_PASSWORD);
    login(INITIAL_EMAIL, INITIAL_PASSWORD);
    visit();
  });

  afterEach(() => {
    deleteUserSql(INITIAL_EMAIL);
    deleteUserSql(NEW_EMAIL);
    deleteAllEmails();
    enableEmailVerification();
  });

  it('email verification on user update works', () => {
    cy.get('form').findInputByName('email').clear().type(NEW_EMAIL);
    cy.contains(EMAIL_VERIFICATION_TEXT).should('be.visible');
    cy.xpath("//*[@name='currentPassword']").clear().type(INITIAL_PASSWORD);
    cy.gcy('global-form-save-button').click();
    cy.contains('Email waiting for verification: pavel@honza.com').should(
      'be.visible'
    );
    getParsedEmailVerification().then((v) => {
      cy.visit(v.verifyEmailLink);
      assertMessage('Email was verified');
      visit();
      cy.contains('Email waiting for verification: pavel@honza.com').should(
        'not.exist'
      );
      cy.get('form').findInputByName('email').should('have.value', NEW_EMAIL);
    });
  });

  it('email change verification is sent to the new email', () => {
    cy.get('form').findInputByName('email').clear().type(NEW_EMAIL);
    cy.xpath("//*[@name='currentPassword']").clear().type(INITIAL_PASSWORD);
    cy.gcy('global-form-save-button').click();
    cy.contains('Email waiting for verification: pavel@honza.com').should(
      'be.visible'
    );

    getParsedEmailVerification().then((v) => {
      cy.wrap(v.toAddress).should('eq', NEW_EMAIL);
    });
  });

  it('resend after email change sends to the new email', () => {
    deleteAllEmails();

    // Change email
    cy.get('form').findInputByName('email').clear().type(NEW_EMAIL);
    cy.xpath("//*[@name='currentPassword']").clear().type(INITIAL_PASSWORD);
    cy.gcy('global-form-save-button').click();
    waitForGlobalLoading();
    cy.contains('Email waiting for verification: pavel@honza.com').should(
      'be.visible'
    );

    // User gets redirected to the verification screen, click resend
    deleteAllEmails();
    cy.visit(HOST + '/projects');
    cy.gcy('resend-email-button').click();
    waitForGlobalLoading();

    // Resent email should go to the NEW email, not the old one
    getParsedEmailVerification().then((v) => {
      cy.wrap(v.toAddress).should('eq', NEW_EMAIL);

      // Verify the link works
      cy.visit(v.verifyEmailLink);
      assertMessage('Email was verified');
    });
  });

  it('works without email verification enabled', () => {
    disableEmailVerification();
    cy.reload();
    cy.get('form').findInputByName('email').clear().type(NEW_EMAIL);
    cy.contains(EMAIL_VERIFICATION_TEXT).should('not.exist');
    cy.xpath("//*[@name='currentPassword']").clear().type(INITIAL_PASSWORD);
    cy.gcy('global-form-save-button').click();
    cy.waitForDom();
    assertMessage('User data updated');
    cy.get('form').findInputByName('email').should('have.value', NEW_EMAIL);
  });

  it('will change user settings', () => {
    cy.visit(`${HOST}/account/profile`);
    cy.contains('User profile').should('be.visible');
    cy.xpath("//*[@name='name']").clear().type('New name');
    cy.contains('Save').click();
    cy.contains('User data updated').should('be.visible');
    cy.reload();
    cy.contains('User profile').should('be.visible');
    cy.xpath("//*[@name='name']").should('have.value', 'New name');
    cy.xpath("//*[@name='email']").should('have.value', 'honza@honza.com');
  });

  it('profile visibility switch waits for the first contribution', () => {
    profile
      .getVisibilitySwitchInput()
      .should('be.disabled')
      .and('not.be.checked');
    cy.gcy('user-profile-visibility-find-link').should('be.visible');
    cy.gcy('user-profile-visibility-view-link').should('not.exist');
  });

  it('will fail when user exists', () => {
    createUser(NEW_EMAIL);
    cy.visit(`${HOST}/account/profile`);
    cy.contains('User profile').should('be.visible');
    cy.xpath("//*[@name='name']").clear().type('New name');
    cy.xpath("//*[@name='email']").clear().type(NEW_EMAIL);
    cy.xpath("//*[@name='currentPassword']").clear().type(INITIAL_PASSWORD);
    cy.contains('Save').click();
    cy.contains('User name already exists.').should('be.visible');
    cy.reload();
    cy.xpath("//*[@name='email']").should('have.value', INITIAL_EMAIL);
  });
});

describe('Profile visibility of a contributor', () => {
  beforeEach(() => {
    communityStatsData.clean();
    communityStatsData.generateStandard();
    login('alice@community.test', 'admin');
    cy.visit(HOST + '/account/profile');
  });

  afterEach(() => {
    communityStatsData.clean();
  });

  it('turns the profile private and back on as default', () => {
    profile.getVisibilitySwitchInput().should('be.enabled').and('be.checked');
    cy.gcy('user-profile-visibility-view-link').should('be.visible');
    cy.gcy('user-profile-visibility-find-link').should('not.exist');

    profile.getVisibilitySwitch().click();
    profile.getVisibilitySwitchInput().should('not.be.checked');
    cy.gcy('global-form-save-button').click();
    assertMessage('User data updated');

    cy.reload();
    profile
      .getVisibilitySwitchInput()
      .should('be.enabled')
      .and('not.be.checked');
    cy.gcy('user-profile-visibility-view-link').should('be.visible');

    cy.intercept('PUT', '**/v2/user').as('saveUser');
    profile.getVisibilitySwitch().click();
    cy.gcy('global-form-save-button').click();
    cy.wait('@saveUser')
      .its('request.body.profileVisibility')
      .should('eq', 'DEFAULT');
    assertMessage('User data updated');
    cy.reload();
    profile.getVisibilitySwitchInput().should('be.enabled').and('be.checked');
  });

  it('a private profile without a counted contribution stays editable', () => {
    profile.getVisibilitySwitch().click();
    cy.gcy('global-form-save-button').click();
    assertMessage('User data updated');
    cy.intercept('GET', '**/v2/public/initial-data', (req) =>
      req.continue((res) => {
        res.body.hasPublicContribution = false;
      })
    );
    cy.reload();
    profile
      .getVisibilitySwitchInput()
      .should('be.enabled')
      .and('not.be.checked');
    cy.gcy('user-profile-visibility-find-link').should('not.exist');

    profile.getVisibilitySwitch().click();
    cy.gcy('user-profile-visibility-find-link').should('be.visible');
  });

  it('keeps an explicitly public profile public without a contribution', () => {
    login('henry.public@community.test', 'admin');
    cy.visit(HOST + '/account/profile');
    profile.getVisibilitySwitchInput().should('be.enabled').and('be.checked');

    profile.getVisibilitySwitch().click();
    profile
      .getVisibilitySwitchInput()
      .should('be.enabled')
      .and('not.be.checked');
    profile.getVisibilitySwitch().click();
    cy.gcy('global-form-save-button').click();
    assertMessage('User data updated');

    cy.reload();
    profile.getVisibilitySwitchInput().should('be.enabled').and('be.checked');
  });
});
