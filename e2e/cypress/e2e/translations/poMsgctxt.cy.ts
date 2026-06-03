import { poMsgctxtTestData } from '../../common/apiCalls/testData/testData';
import { login } from '../../common/apiCalls/common';
import { visitTranslations } from '../../common/translations';
import { waitForGlobalLoading } from '../../common/loading';
import { E2TranslationsView } from '../../compounds/E2TranslationsView';

const PO_MSGCTXT_KEY_SEPARATOR = '\u0004';

describe('PO msgctxt key names', () => {
  let projectId: number;

  beforeEach(() => {
    poMsgctxtTestData.clean({ failOnStatusCode: false });
    poMsgctxtTestData
      .generateStandard()
      .then((r) => {
        projectId = r.body.projects[0].id;
      })
      .then(() => login('test_username'));
  });

  afterEach(() => {
    poMsgctxtTestData.clean({ failOnStatusCode: false });
  });

  it('renders msgctxt as chip in the translations grid', () => {
    visitTranslations(projectId);
    waitForGlobalLoading();

    cy.gcy('translations-key-name')
      .contains('Open')
      .closest('[data-cy="translations-key-name"]')
      .findDcy('key-name-msgctxt')
      .should('be.visible')
      .and('have.text', 'menu');

    cy.gcy('translations-key-name')
      .contains('plain.key')
      .closest('[data-cy="translations-key-name"]')
      .findDcy('key-name-msgctxt')
      .should('not.exist');
  });

  it('pasting EOT into the key-name editor renders an msgctxt chip', () => {
    visitTranslations(projectId);
    waitForGlobalLoading();

    const translationsView = new E2TranslationsView();
    const dialog = translationsView.openKeyCreateDialog();

    dialog.getKeyNameInput().find('.cm-content').as('keyInput');

    cy.get('@keyInput').click();
    cy.get('@keyInput').then(($el) => {
      const text = `pasted${PO_MSGCTXT_KEY_SEPARATOR}Save`;
      const clipboardData = new DataTransfer();
      clipboardData.setData('text/plain', text);
      const event = new ClipboardEvent('paste', {
        clipboardData,
        bubbles: true,
        cancelable: true,
      });
      $el[0].dispatchEvent(event);
    });

    dialog
      .getKeyNameInput()
      .find('.keyname-msgctxt-widget')
      .should('be.visible')
      .and('have.text', 'pasted');
  });
});
