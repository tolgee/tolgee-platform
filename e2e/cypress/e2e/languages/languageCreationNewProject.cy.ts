import { HOST } from '../../common/constants';
import {
  dismissMenu,
  gcy,
  getInputByName,
  getPopover,
  selectInProjectMenu,
  selectInSelect,
} from '../../common/shared';
import {
  selectInAutocomplete,
  setLanguageData,
  typeToAutocomplete,
} from '../../common/languages';
import { languagesTestData } from '../../common/apiCalls/testData/testData';
import { login } from '../../common/apiCalls/common';
import { waitForGlobalLoading } from '../../common/loading';

describe('Language creation in new project', () => {
  beforeEach(() => {
    languagesTestData.clean();
    languagesTestData.generate().then(() => {
      login('franta');
      cy.visit(`${HOST}/projects/add`);
    });
  });

  it('adds languages', () => {
    addLanguage('Azerbaijani');
    addLanguage('Deutsch');
    assertContainsLanguage('Azerbaijani | azərbaycan (az)');
    assertContainsLanguage('English | English (en)');
    assertContainsLanguage('German (Germany) | Deutsch (Deutschland) (de-DE)');
  });

  it('removes languages', () => {
    addLanguage('Azerbaijani');
    cy.contains('Azerbaijani | azərbaycan (az)').should('be.visible');
    addLanguage('Deutsch');
    removeLanguage('Azerbaijani');
    cy.contains('Azerbaijani | azərbaycan (az)').should('not.exist');
  });

  it('modifies languages', () => {
    addLanguage('Azerbaijani');
    addLanguage('Deutsch');
    getPreparedLanguage('Azerbaijani')
      .findDcy('languages-create-customize-button')
      .click();
    setLanguageData({
      name: 'Modified',
      originalName: 'modified orig',
      tag: 'az-mod',
      flagEmoji: '🇨🇭',
    });
    gcy('languages-modify-apply-button').click();
    assertContainsLanguage('Modified | modified orig (az-mod)');
    gcy('languages-prepared-language-box').should('have.length', 3);
  });

  it('cancels modification', () => {
    addLanguage('Azerbaijani');
    addLanguage('Deutsch');
    getPreparedLanguage('Azerbaijani')
      .findDcy('languages-create-customize-button')
      .click();
    gcy('language-modify-form').should('be.visible');
    setLanguageData({
      name: 'Modified',
      originalName: 'modified orig',
      tag: 'az-mod',
      flagEmoji: '🇨🇭',
    });
    gcy('languages-modify-cancel-button').click();
    gcy('language-modify-form').should('not.exist');
    assertContainsLanguage('Azerbaijani | azərbaycan (az)');
  });

  it('updates base languages on add', () => {
    addLanguage('Azerbaijani');
    gcy('base-language-select').click();
    getPopover().contains('English').should('be.visible');
    getPopover().contains('Azerbaijani').should('be.visible');
    dismissMenu();
    addLanguage('Deutsch');
    gcy('base-language-select').click();
    getPopover().contains('German').should('be.visible');
  });

  it('updates base languages on remove', () => {
    addLanguage('Azerbaijani');
    gcy('base-language-select').click();
    getPopover().contains('English').should('be.visible');
    getPopover().contains('Azerbaijani').should('be.visible');
    dismissMenu();
    removeLanguage('Azerbaijani');
    gcy('base-language-select').click();
    getPopover().contains('Azerbaijani').should('not.exist');
  });

  it('select other language when base language is removed', () => {
    addLanguage('Azerbaijani');
    gcy('base-language-select').click();
    getPopover().contains('English').should('be.visible');
    getPopover().contains('Azerbaijani').should('be.visible');
    dismissMenu();
    removeLanguage('English');
    gcy('base-language-select').contains('Azerbaijani').should('be.visible');
  });

  it('validates language repeat', () => {
    getInputByName('name').type('Super project');
    typeToAutocomplete('English');
    gcy('languages-create-autocomplete-suggested-option')
      .contains('English')
      .should('have.css', 'pointer-events', 'none');
    cy.gcy('languages-prepared-language-box').should('have.length', 1);
  });

  it('validates no languages', () => {
    removeLanguage('English');
    getInputByName('name').type('aa');
    gcy('global-form-save-button').click();
    cy.contains('Add at least one language').should('be.visible');
  });

  it('creates a project with languages', () => {
    const languagesToAdd = ['German', 'Hindi', 'Czech'];
    languagesToAdd.forEach((l) => addLanguage(l));
    getInputByName('name').type('Super project');
    selectInSelect(gcy('base-language-select'), 'German');
    gcy('global-form-save-button').click();
    waitForGlobalLoading();
    selectInProjectMenu('Languages');
    languagesToAdd.forEach((l) =>
      gcy('project-settings-languages').contains(l).should('be.visible')
    );
  });
});

const addLanguage = (language: string) => {
  typeToAutocomplete(language);
  selectInAutocomplete(language);
};

const getPreparedLanguage = (label: string) => {
  return gcy('languages-prepared-language-box')
    .contains(label)
    .closestDcy('languages-prepared-language-box');
};

const removeLanguage = (label: string) =>
  getPreparedLanguage(label)
    .findDcy('languages-create-cancel-prepared-button')
    .click();

const assertContainsLanguage = (label: string) => {
  getPreparedLanguage(label).should('be.visible');
};
