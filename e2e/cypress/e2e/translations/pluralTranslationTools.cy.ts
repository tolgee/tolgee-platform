import { ProjectDTO } from '../../../../webapp/src/service/response.types';
import { createKey, deleteProject } from '../../common/apiCalls/common';
import { waitForGlobalLoading } from '../../common/loading';
import {
  getPluralEditor,
  getTranslationCell,
  selectLangsInLocalstorage,
  translationsBeforeEach,
  visitTranslations,
} from '../../common/translations';

describe('translation tools panel with plurals', () => {
  let project: ProjectDTO = null;

  beforeEach(() => {
    translationsBeforeEach()
      .then((p) => (project = p))
      .then(() => {
        selectLangsInLocalstorage(project.id, ['en', 'cs']);
      });
  });

  afterEach(() => {
    deleteProject(project.id);
  });

  it('will suggest correctly from MT', () => {
    createMTKeys();
    visit();

    // check variant "one"
    getTranslationCell('mt key 1', 'cs').click();
    getPluralEditor('one').click();
    waitForGlobalLoading();
    cy.gcy('translation-tools-machine-translation-item')
      .contains('#1 item translated with GOOGLE from en to cs')
      .should('be.visible')
      .click();

    // check variant "few"
    getPluralEditor('few').click();
    waitForGlobalLoading();
    cy.gcy('translation-tools-machine-translation-item')
      .contains('#2 items translated with GOOGLE from en to cs')
      .should('be.visible')
      .click();

    // check variant "other"
    getPluralEditor('other').click();
    waitForGlobalLoading();
    cy.gcy('translation-tools-machine-translation-item')
      .contains('#10 items translated with GOOGLE from en to cs')
      .should('be.visible')
      .click();

    cy.gcy('translations-cell-main-action-button').click();
    waitForGlobalLoading();
    cy.gcy('global-editor').should('not.exist');
  });

  it('will suggest correctly from TM', () => {
    createTMKeys();
    visit();

    // check variant "one"
    getTranslationCell('tm key 1', 'cs').click();
    getPluralEditor('one').click();
    waitForGlobalLoading();
    cy.gcy('translation-tools-translation-memory-item')
      .contains('#1 položka')
      .should('be.visible')
      .click();

    // check variant "few"
    getPluralEditor('few').click();
    waitForGlobalLoading();
    cy.gcy('translation-tools-translation-memory-item')
      .contains('#2 položky')
      .should('be.visible')
      .click();

    // check variant "other"
    getPluralEditor('other').click();
    waitForGlobalLoading();
    cy.gcy('translation-tools-translation-memory-item')
      .contains('#10 položek')
      .should('be.visible')
      .click();

    cy.gcy('translations-cell-main-action-button').click();
    waitForGlobalLoading();
    cy.gcy('global-editor').should('not.exist');
  });

  function createTMKeys() {
    createKey(
      project.id,
      'tm key 1',
      {
        en: '{value, plural, one {# item} other {# items}}',
      },
      { isPlural: true }
    );

    return createKey(
      project.id,
      'tm key 2',
      {
        en: '{value, plural, one {# item} other {# items}}',
        cs: '{value, plural, one {# položka} few {# položky} other {# položek}}',
      },
      { isPlural: true }
    );
  }

  function createMTKeys() {
    return createKey(
      project.id,
      'mt key 1',
      {
        en: '{value, plural, one {# item} other {# items}}',
      },
      { isPlural: true }
    );
  }

  const visit = () => {
    visitTranslations(project.id);
  };
});
