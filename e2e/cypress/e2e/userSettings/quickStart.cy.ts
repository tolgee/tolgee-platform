import {
  deleteUserSql,
  disableEmailVerification,
  setRecaptchaSecretKey,
} from '../../common/apiCalls/common';
import { fillAndSubmitSignUpForm, visitSignUp } from '../../common/login';
import {
  assertMessage,
  dismissMenu,
  gcy,
  gcyAdvanced,
} from '../../common/shared';
import type {
  HighlightItem,
  ItemStep,
} from '../../../../webapp/src/component/layout/QuickStartGuide/enums';

describe('Quick start guide', () => {
  const INITIAL_EMAIL = 'quick-start-test@test.com';

  beforeEach(() => {
    deleteUserSql(INITIAL_EMAIL);
    visitSignUp();
    setRecaptchaSecretKey('dummy_secret_key');
    disableEmailVerification();
    fillAndSubmitSignUpForm(INITIAL_EMAIL, true);
  });

  it('goes through quick start guide without problems', () => {
    // demo project
    gcy('quick-start-action').contains('Try demo').click();
    getHighlight('demo_project').click();
    assertStepComplete('new_project');

    // create project
    gcy('quick-start-action').contains('Create project').click();
    getHighlight('add_project').click();
    gcy('project-name-field').type('Test');
    gcy('global-form-save-button').click();
    assertMessage('Project created!');

    // setup languages
    gcy('quick-start-action').contains('Set up').click();
    getHighlight('menu_languages').click();
    getHighlightOkButton('add_language').click();
    cy.get('#machine-translation-tab').click();
    getHighlightOkButton('machine_translation').click();
    assertStepComplete('languages');

    // invite members
    gcy('quick-start-action').contains('Invite').click();
    getHighlight('menu_members').click();
    getHighlightOkButton('invitations').click();
    getHighlightOkButton('members').click();
    assertStepComplete('members');

    // add keys
    gcy('quick-start-action').contains('Add keys').click();
    getHighlight('menu_translations').click();
    getHighlight('add_key').click();
    dismissMenu();
    assertStepComplete('keys');

    // import
    gcy('quick-start-action').contains('Import').click();
    getHighlight('menu_import').click();
    getHighlightOkButton('pick_import_file').click();

    // integrate
    gcy('quick-start-action').contains('Integrate').click();
    getHighlight('menu_integrate').click();
    getHighlightOkButton('integrate_form').click();
    assertStepComplete('use');

    // production
    gcy('quick-start-action').contains('Content delivery').click();
    getHighlight('menu_developer').click();
    getHighlightOkButton('content_delivery_page').click();
    assertStepComplete('production');

    // export
    gcy('quick-start-action').contains('Export').click();
    getHighlight('menu_export').click();
    getHighlightOkButton('export_form').click();

    // close guide
    gcy('quick-start-finish-action').click();
    gcy('quick-start-dialog').should('not.exist');
    cy.reload();
    gcy('quick-start-dialog').should('not.exist');
  });

  function getHighlight(item: HighlightItem) {
    return gcyAdvanced({
      value: 'quick-start-highlight',
      item,
    })
      .children()
      .first();
  }

  function getHighlightOkButton(item: HighlightItem) {
    return gcyAdvanced({ value: 'quick-start-highlight-ok', item });
  }

  function assertStepComplete(step: ItemStep) {
    return gcyAdvanced({ value: 'quick-start-step', step }).should(
      'have.class',
      'done'
    );
  }
});
