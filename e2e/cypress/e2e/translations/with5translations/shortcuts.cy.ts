import { ProjectDTO } from '../../../../../webapp/src/service/response.types';

import {
  editCell,
  IS_MAC,
  move,
  selectFirst,
  shortcut,
} from '../../../common/shortcuts';
import { assertHasState } from '../../../common/state';
import {
  create4Translations,
  selectLangsInLocalstorage,
  translationsBeforeEach,
} from '../../../common/translations';

describe('Shortcuts', () => {
  let project: ProjectDTO = null;

  beforeEach(() => {
    translationsBeforeEach()
      .then((p) => {
        project = p;
        selectLangsInLocalstorage(project.id, ['en', 'cs']);
      })
      .then(() => create4Translations(project.id));
    cy.contains('Cool translated text 1').should('be.visible');
  });

  it('will navigate through list correctly', () => {
    selectFirst();
    move('uparrow', 'Cool translated text 1');
    move('rightarrow', 'Cool translated text 1');
    move('downarrow', 'Studený přeložený text 1');
    move('downarrow', 'Cool translated text 2');
    move('downarrow', 'Studený přeložený text 2');
    move('leftarrow', 'Cool key 02');
    move('downarrow', 'Cool key 03');
    move('downarrow', 'Cool key 04');
    move('downarrow', 'Cool key 04');
    move('leftarrow', 'Cool key 04');
  });

  it('will navigate through table correctly', () => {
    cy.gcy('translations-view-table-button').click();

    selectFirst();
    move('uparrow', 'Cool translated text 1');
    move('rightarrow', 'Studený přeložený text 1');
    move('rightarrow', 'Studený přeložený text 1');
    move('downarrow', 'Studený přeložený text 2');
    move('leftarrow', 'Cool translated text 2');
    move('leftarrow', 'Cool key 02');
    move('downarrow', 'Cool key 03');
    move('downarrow', 'Cool key 04');
    move('downarrow', 'Cool key 04');
    move('leftarrow', 'Cool key 04');
  });

  it('will change cell state', () => {
    selectFirst();
    move('downarrow', 'Studený přeložený text 1');
    const action = IS_MAC ? '{cmd}' : '{ctrl}';
    shortcut([action, 'e']);
    assertHasState('Studený přeložený text 1', 'Reviewed');
    shortcut([action, 'e']);
    assertHasState('Studený přeložený text 1', 'Translated');
  });

  it(
    'will keep focus after edit',
    {
      retries: {
        runMode: 4,
      },
    },
    () => {
      // edit with changes
      selectFirst();
      editCell('Cool translated text 1', 'Yo, new cool text', true);
      cy.focused().contains('Yo, new cool text').should('be.visible');

      move('downarrow');

      // edit without chages
      editCell('Studený přeložený text 1', 'Studený přeložený text 1', true);
      cy.focused().contains('Studený přeložený text 1').should('be.visible');

      move('downarrow');

      // cancel edit
      editCell('Cool translated text 2', 'Yo, new cool text', false);
      cy.focused().type('{esc}');
      cy.wait(100);
      cy.focused().contains('Cool translated text 2').should('be.visible');
    }
  );
});
