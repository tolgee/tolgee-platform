import {
  getCustomNameInput,
  selectInAutocomplete,
  setLanguageData,
  typeToAutocomplete,
} from '../../common/languages';
import {
  assertMessage,
  gcy,
  getInputByName,
  visitProjectLanguages,
} from '../../common/shared';
import { languagesTestData } from '../../common/apiCalls/testData/testData';
import { login } from '../../common/apiCalls/common';

describe('Language creation', () => {
  beforeEach(() => {
    languagesTestData.clean();

    languagesTestData.generate().then((languageData) => {
      login('franta');
      visitProjectLanguages(languageData.body.id);
    });
  });

  it('adds language', () => {
    prepareCzechLanguage();
    gcy('languages-add-dialog-submit').click();
    gcy('project-settings-languages').should('contain', 'Czech');
    gcy('project-settings-languages').should('contain', 'čeština');
    assertMessage('Languages created');
  });

  it('customizes language', () => {
    prepareCzechLanguage();
    gcy('languages-create-customize-button').click();
    setLanguageData({
      name: 'Czech modified',
      originalName: 'Česky upraveno',
      tag: 'cs-mod',
      flagEmoji: '🇨🇭',
    });
    cy.gcy('languages-modify-apply-button').click();
    cy.gcy('languages-prepared-language-box').should(
      'contain',
      'Czech modified'
    );
    cy.gcy('languages-prepared-language-box').should(
      'contain',
      'Česky upraveno'
    );
    cy.gcy('languages-prepared-language-box').should('contain', 'cs-mod');
    gcy('languages-add-dialog-submit').click();
    assertMessage('Languages created');
  });

  it('custom language can be created', () => {
    addCustomLanguage();
    getCustomNameInput().should('be.visible');
    getInputByName('originalName').type('New custom lang');
    cy.gcy('languages-modify-apply-button').click();
    cy.gcy('languages-prepared-language-box').should(
      'contain',
      'New custom lang'
    );
  });

  it('validates tag', () => {
    addCustomLanguage();
    getInputByName('tag').type('!');
    cy.contains(
      "This language tag doesn't follow BCP 47 standard. Consider providing a valid tag."
    ).should('be.visible');
  });

  it('cancels modification of invalid tag properly', () => {
    addCustomLanguage();
    gcy('languages-modify-cancel-button').click();
    //originalName is required, so it should return user back to autocomplete
    gcy('languages-create-autocomplete-field').should('be.visible');
  });

  it('cancels prepared language', () => {
    prepareCzechLanguage();
    gcy('languages-create-cancel-prepared-button').click();
    gcy('languages-create-autocomplete-field').should('be.visible');
  });

  after(() => {
    languagesTestData.clean();
  });
});

const addCustomLanguage = () => {
  gcy('project-settings-languages-add').click();
  typeToAutocomplete('cs');
  selectInAutocomplete('New custom language');
};

const prepareCzechLanguage = () => {
  gcy('project-settings-languages-add').click();
  typeToAutocomplete('cs');
  selectInAutocomplete('čeština');
};
