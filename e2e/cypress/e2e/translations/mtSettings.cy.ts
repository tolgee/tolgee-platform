import { ProjectDTO } from '../../../../webapp/src/service/response.types';
import {
  createProjectWithThreeLanguages,
  createTranslation,
} from '../../common/translations';
import { deleteProject } from '../../common/apiCalls/common';
import { HOST } from '../../common/constants';
import { waitForGlobalLoading } from '../../common/loading';
import { gcyAdvanced } from '../../common/shared';

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

  it('will update default settings', { retries: 5 }, () => {
    cy.gcy('machine-translations-settings-language-options').click();
    getEnableCheckbox('GOOGLE').click();
    getPrimaryRadio('AWS').click();
    cy.gcy('mt-language-dialog-auto-for-import').click();
    cy.gcy('mt-language-dialog-auto-machine-translation').click();
    cy.gcy('mt-language-dialog-auto-translation-memory').click();
    cy.gcy('mt-language-dialog-save').click();

    waitForGlobalLoading();

    getAvatarPrimary('AWS', 'default').should('be.visible');
    getAvatarEnabled('AWS', 'default').should('be.visible');

    cy.gcy('project-menu-item-translations').click();

    openEditor('Studený přeložený text 1');
    cy.gcy('translation-tools-machine-translation-item')
      .contains('Cool translated text 1 translated with AWS from en to cs')
      .should('be.visible');
    cy.gcy('translation-tools-machine-translation-item').should(
      'have.length',
      1
    );
    cy.gcy('global-editor').type('{esc}');
    createTranslation('aaa_key', 'test translation');
    cy.contains('test translation translated with AWS from en to cs').should(
      'be.visible'
    );
  });

  it('will update language specific settings', { retries: 5 }, () => {
    cy.gcy('machine-translations-settings-toggle').click();
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
        'Cool translated text 1 translated with AWS from en to es FORMAL'
      )
      .should('be.visible');
    cy.gcy('global-editor').type('{esc}');

    openEditor('Studený přeložený text 1');
    cy.gcy('translation-tools-machine-translation-item').should(
      'have.length',
      2
    );
    cy.gcy('global-editor').type('{esc}');

    createTranslation('aaa_key', 'test translation');
    cy.contains(
      'test translation translated with AWS from en to es FORMAL'
    ).should('be.visible');
    cy.contains('from en to cs').should('not.exist');
  });

  const visit = () => {
    cy.visit(`${HOST}/projects/${project.id}/languages`);
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
});
