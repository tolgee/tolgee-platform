import { login } from '../../common/apiCalls/common';
import { copyTranslationTestData } from '../../common/apiCalls/testData/testData';
import {
  forEachView,
  getTranslationCell,
  selectLangsInLocalstorage,
  visitTranslations,
} from '../../common/translations';
import { waitForGlobalLoading } from '../../common/loading';
import { stubClipboardNow } from '../../common/clipboard';

const NORMALIZED_PLURAL_ICU = '{value, plural,\none {# dog}\nother {# dogs}\n}';

describe('Translation copy button', () => {
  let projectId: number;

  beforeEach(() => {
    copyTranslationTestData.clean({ failOnStatusCode: false });
    copyTranslationTestData
      .generateStandard()
      .then((r) => r.body)
      .then((data) => {
        login(data.users[0].username);
        projectId = data.projects[0].id;
        selectLangsInLocalstorage(projectId, ['en', 'cs']);
        visitTranslations(projectId);
        waitForGlobalLoading();
      });
  });

  afterEach(() => {
    copyTranslationTestData.clean();
  });

  forEachView(() => {
    it('copies the translation without opening the editor', () => {
      const copied = stubClipboardNow();

      getTranslationCell('Test key', 'en')
        .trigger('mouseover')
        .findDcy('translations-cell-copy-button')
        .click();

      cy.gcy('global-editor').should('not.exist');
      getTranslationCell('Test key', 'en')
        .findDcyAdvanced({
          value: 'translations-cell-copy-button',
          copied: 'true',
        })
        .should('be.visible');
      cy.then(() => expect(copied.text).to.equal('Translated test key'));
    });

    it('copies the raw ICU message of a plural key', () => {
      const copied = stubClipboardNow();

      getTranslationCell('Plural key', 'en')
        .trigger('mouseover')
        .findDcy('translations-cell-copy-button')
        .click();

      cy.then(() => expect(copied.text).to.equal(NORMALIZED_PLURAL_ICU));
    });

    it('has no copy button on an untranslated cell', () => {
      getTranslationCell('Test key', 'cs')
        .trigger('mouseover')
        .findDcy('translations-cell-edit-button')
        .should('exist');

      getTranslationCell('Test key', 'cs')
        .findDcy('translations-cell-copy-button')
        .should('not.exist');
    });
  });
});
