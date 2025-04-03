import { getAnyContainingText } from '../../../common/xPath';
import { ProjectDTO } from '../../../../../webapp/src/service/response.types';
import {
  confirmDiscard,
  create4Translations,
  editCell,
  editKeyName,
  forEachView,
  getCell,
  getCellCancelButton,
  getCellInsertBaseButton,
  getCellSaveButton,
  openKeyEditDialog,
  selectLangsInLocalstorage,
  translationsBeforeEach,
  typeNewKeyName,
  visitTranslations,
} from '../../../common/translations';
import { gcy } from '../../../common/shared';
import { enableNamespaces } from '../../../common/apiCalls/common';
import { selectNamespace } from '../../../compounds/E2NamespaceSelector';

describe('Views with 5 Translations', () => {
  let project: ProjectDTO = null;

  beforeEach(() => {
    translationsBeforeEach()
      .then((p) => (project = p))
      .then(() => create4Translations(project.id))
      .then(() => {
        visitTranslations(project.id);
      });
  });

  // run same tests for list view and table view
  forEachView(
    () => project.id,
    () => {
      it('edits key', () => {
        editKeyName('Cool key 01', 'Cool key edited');

        cy.contains('Cool key edited').should('be.visible');
        cy.contains('Cool key 02').should('be.visible');
        cy.contains('Cool key 04').should('be.visible');
      });

      it('inserts base into translation', () => {
        selectLangsInLocalstorage(project.id, ['en', 'cs']);
        visitTranslations(project.id);

        editCell('Studený přeložený text 1');
        getCellInsertBaseButton().click();

        cy.gcy('global-editor')
          .first()
          .contains('Cool translated text 1')
          .should('be.visible');
      });

      it('edits translation', () => {
        editCell('Cool translated text 1', 'Super cool changed text...');
        cy.xpath(
          `${getAnyContainingText(
            'Super cool changed text...'
          )}/parent::*//button[@type='submit']`
        ).should('not.exist');
        cy.contains('Super cool changed text...').should('be.visible');
        cy.contains('Cool translated text 2').should('be.visible');
      });

      it('edits key namespace', () => {
        enableNamespaces(project.id);
        getCell('Cool key 01').click();

        selectNamespace('test-ns');

        getCellSaveButton().click();

        gcy('translations-namespace-banner')
          .contains('test-ns')
          .should('be.visible');

        gcy('translations-namespace-banner')
          .contains('<none>')
          .should('be.visible');
      });

      it('cancels key edit without confirmation', () => {
        openKeyEditDialog('Cool key 01');
        typeNewKeyName('Cool key edited');

        getCellCancelButton().click();

        cy.contains('Cool key edited').should('not.exist');
        cy.contains('Cool key 01').should('be.visible');
      });

      it('asks for confirmation on changed edit', () => {
        editCell('Cool translated text 1', 'Cool translation edited', false);
        cy.contains('Cool translated text 4').click();
        cy.contains(`Discard changes?`).should('be.visible');
        confirmDiscard();
        cy.gcy('global-editor')
          .contains('Cool translation edited')
          .should('not.exist');
        cy.gcy('global-editor')
          .contains('Cool translated text 4')
          .should('be.visible');
      });
    }
  );
});
