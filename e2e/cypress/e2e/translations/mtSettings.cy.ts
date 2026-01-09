import { ProjectDTO } from '../../../../webapp/src/service/response.types';
import {
  createProjectWithThreeLanguages,
  createTranslation,
} from '../../common/translations';
import { deleteProject } from '../../common/apiCalls/common';
import { HOST } from '../../common/constants';
import { waitForGlobalLoading } from '../../common/loading';
import { gcyAdvanced } from '../../common/shared';
import { buildXpath } from '../../common/XpathBuilder';

describe('Machine translation settings', () => {
  let project: ProjectDTO = null;

  beforeEach(() => {
    createProjectWithThreeLanguages().then((p) => {
      project = p;
      visit();
    });
  });

  afterEach(() => {
    if (project) {
      deleteProject(project.id);
    }
  });

  it('updates default settings', { retries: 5 }, () => {
    cy.gcy('machine-translations-settings-language-options').first().click();
    getEnableCheckbox('GOOGLE').click();
    getPrimaryRadio('AWS').click();
    getFormalitySelect('AWS').click();
    cy.gcy('mt-language-dialog-formality-select-item')
      .contains('Formal')
      .click();
    cy.gcy('mt-language-dialog-auto-for-import').click();
    cy.gcy('mt-language-dialog-auto-machine-translation').click();
    cy.gcy('mt-language-dialog-auto-translation-memory').click();
    cy.gcy('mt-language-dialog-save').click();

    waitForGlobalLoading();

    getAvatarPrimary('AWS', 'default').should('be.visible');
    getAvatarEnabled('AWS', 'default').should('be.visible');

    cy.gcy('project-menu-item-translations').click();

    openEditor('Studený přeložený text 1');
    assertSingleMtTranslation();
  });

  it('updates language specific settings', { retries: 5 }, () => {
    gcyAdvanced({
      value: 'machine-translations-settings-language-options',
      language: 'es',
    }).click();

    getPrimaryRadio('AWS').click();
    getFormalitySelect('AWS').click();
    cy.gcy('mt-language-dialog-formality-select-item')
      .contains('Formal')
      .click();

    cy.gcy('mt-language-dialog-auto-for-import').click();
    cy.gcy('mt-language-dialog-auto-machine-translation').click();
    cy.gcy('mt-language-dialog-auto-translation-memory').click();
    cy.gcy('mt-language-dialog-save').click();

    waitForGlobalLoading();

    getAvatarPrimary('AWS', 'es').should('be.visible');
    getAvatarEnabled('GOOGLE', 'es').should('be.visible');

    cy.gcy('project-menu-item-translations').click();
    openEditor('Texto traducido en frío 1');

    cy.gcy('translation-tools-machine-translation-item').should(
      'have.length',
      2
    );
    cy.gcy('translation-tools-machine-translation-item')
      .contains(
        'Cool translated text 1 translated FORMAL with AWS from en to es'
      )
      .should('be.visible');
    cy.gcy('global-editor').type('{esc}');

    openEditor('Studený přeložený text 1');
    cy.gcy('translation-tools-machine-translation-item').should(
      'have.length',
      2
    );
    cy.gcy('global-editor').type('{esc}');

    createTranslation({ key: 'aaa_key', translation: 'test translation' });
    cy.contains(
      'test translation translated FORMAL with AWS from en to es'
    ).should('be.visible');
    cy.contains('from en to cs').should('not.exist');
  });

  it(
    'preserves settings across language configurations',
    { retries: 5 },
    () => {
      // This checks for the case where modifying one language
      // doesn't affect the other languages.
      // To be exact, the Spanish language supports formality, while
      // the Czech language doesn't. This used to reset the Spanish
      // settings when changing the Czech settings, because of a bug.

      gcyAdvanced({
        value: 'machine-translations-settings-language-options',
        language: 'es',
      }).click();

      getPrimaryRadio('AWS').click();
      getFormalitySelect('AWS').click();
      cy.gcy('mt-language-dialog-formality-select-item')
        .contains('Formal')
        .click();
      cy.gcy('mt-language-dialog-save').click();

      waitForGlobalLoading();

      getAvatarPrimary('AWS', 'es').should('be.visible');

      gcyAdvanced({
        value: 'machine-translations-settings-language-options',
        language: 'cs',
      }).click();

      getPrimaryRadio('AWS').click();
      cy.gcy('mt-language-dialog-save').click();

      waitForGlobalLoading();

      getAvatarPrimary('AWS', 'cs').should('be.visible');

      // Re-open Spanish settings
      gcyAdvanced({
        value: 'machine-translations-settings-language-options',
        language: 'es',
      }).click();

      // Verify AWS is still primary
      getPrimaryRadio('AWS').find('input').should('be.checked');

      // Verify formality is still set to Formal
      getFormalitySelect('AWS').should('contain', 'Formal');
    }
  );

  const visit = () => {
    cy.visit(`${HOST}/projects/${project.id}/languages/mt`);
  };

  const openEditor = (text: string) => {
    cy.contains(text).click();
    cy.gcy('global-editor').should('be.visible');
  };

  const getEnableCheckbox = (service: string) => {
    return gcyAdvanced({
      value: 'mt-language-dialog-enabled-checkbox',
      service,
    });
  };

  const getPrimaryRadio = (service: string) => {
    return gcyAdvanced({ value: 'mt-language-dialog-primary-radio', service });
  };

  const getFormalitySelect = (service: string) => {
    return gcyAdvanced({
      value: 'mt-language-dialog-formality-select',
      service,
    });
  };

  const getAvatarPrimary = (service: string, language: string) => {
    return gcyAdvanced({
      value: 'machine-translations-settings-language-primary-service',
      service,
      language,
    });
  };

  const getAvatarEnabled = (service: string, language: string) => {
    return gcyAdvanced({
      value: 'machine-translations-settings-language-enabled-service',
      service,
      language,
    });
  };

  function assertSingleMtTranslation(
    text = 'Cool translated text 1 translated FORMAL with AWS from en to cs'
  ) {
    buildXpath()
      .descendantOrSelf()
      .withDataCy('translation-tools-machine-translation-item')
      .descendantOrSelf()
      .containsText(text)
      .getElement()
      .should('be.visible');
    cy.gcy('translation-tools-machine-translation-item').should(
      'have.length',
      1
    );
  }
});
