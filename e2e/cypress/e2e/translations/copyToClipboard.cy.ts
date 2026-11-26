import { login } from '../../common/apiCalls/common';
import {
  translationCopyTestData,
  generatePermissionsData,
} from '../../common/apiCalls/testData/testData';
import { waitForGlobalLoading } from '../../common/loading';
import {
  getTranslationCell,
  getTranslationCopyButton,
  getTranslationVariantCopyButton,
  hoverTranslationCell,
  selectLangsInLocalstorage,
  visitTranslations,
} from '../../common/translations';

const KEY = 'greeting';
const PLURAL_KEY = 'unread-count';
const TEXT = 'Hello {name}, you have <b>new</b> messages';

describe('Copy translation to clipboard', () => {
  describe('as a project owner', () => {
    beforeEach(() => {
      translationCopyTestData.clean({ failOnStatusCode: false });
      translationCopyTestData
        .generateStandard()
        .then((r) => r.body)
        .then((data) => {
          const projectId = data.projects[0].id;
          login(data.users[0].username);
          selectLangsInLocalstorage(projectId, ['en', 'cs']);
          visitTranslations(projectId);
          waitForGlobalLoading();
        });
    });

    afterEach(() => {
      translationCopyTestData.clean();
    });

    ['list', 'table'].forEach((view) => {
      it(`copies the raw text without opening the editor in the ${view} view`, () => {
        if (view === 'table') {
          cy.gcy('translations-view-table-button').click();
        }
        const clipboard = stubClipboard();

        hoverTranslationCell(KEY, 'en');
        getTranslationCopyButton(KEY, 'en').click();

        cy.wrap(clipboard).should('have.property', 'text', TEXT);
        cy.gcy('global-editor').should('not.exist');
      });
    });

    it('confirms the copy and resets the confirmation two seconds later', () => {
      stubClipboard();

      hoverTranslationCell(KEY, 'en');
      getTranslationCopyButton(KEY, 'en')
        .should('have.attr', 'data-cy-copied', 'false')
        .click()
        .should('have.attr', 'data-cy-copied', 'true');

      getTranslationCopyButton(KEY, 'en').should(
        'have.attr',
        'data-cy-copied',
        'false'
      );
    });

    it('copies each plural form on its own', () => {
      const clipboard = stubClipboard();

      hoverTranslationCell(PLURAL_KEY, 'en');
      getTranslationCopyButton(PLURAL_KEY, 'en').should('have.length', 2);

      getTranslationVariantCopyButton(PLURAL_KEY, 'en', 'other').click();
      cy.wrap(clipboard).should('have.property', 'text', '# unread messages');

      getTranslationVariantCopyButton(PLURAL_KEY, 'en', 'one').click();
      cy.wrap(clipboard).should('have.property', 'text', '# unread message');
      cy.gcy('global-editor').should('not.exist');
    });

    it('offers no copy button for an empty translation', () => {
      hoverTranslationCell(KEY, 'cs');
      getTranslationCell(KEY, 'cs')
        .findDcy('translations-cell-comments-button')
        .should('exist');
      getTranslationCopyButton(KEY, 'cs').should('not.exist');
    });
  });

  describe('as a user who can only view translations', () => {
    beforeEach(() => {
      generatePermissionsData.clean();
      generatePermissionsData
        .generate({ scopes: ['translations.view'] })
        .then((res) => {
          const projectId = res.body.projects[0].id;
          login('me@me.me');
          selectLangsInLocalstorage(projectId, ['en', 'de']);
          visitTranslations(projectId);
          waitForGlobalLoading();
        });
    });

    afterEach(() => {
      generatePermissionsData.clean();
    });

    it('offers copy but not edit', () => {
      const clipboard = stubClipboard();

      hoverTranslationCell('key-1', 'de');
      getTranslationCopyButton('key-1', 'de').click();

      cy.wrap(clipboard).should('have.property', 'text', 'German text 1');
      getTranslationCell('key-1', 'de')
        .findDcy('translations-cell-edit-button')
        .should('not.exist');
    });
  });
});

function stubClipboard() {
  const clipboard: { text?: string } = {};
  cy.document().then((doc) => {
    cy.stub(doc, 'execCommand').callsFake((command: string) => {
      if (command === 'copy') {
        clipboard.text = doc.getSelection()?.toString();
      }
      return true;
    });
  });
  return clipboard;
}
